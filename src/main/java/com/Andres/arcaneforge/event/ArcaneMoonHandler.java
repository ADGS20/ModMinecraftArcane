package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.config.ArcaneServerConfig;
import com.Andres.arcaneforge.moon.ArcaneMoonData;
import com.Andres.arcaneforge.moon.ArcaneMoonLogic;
import com.Andres.arcaneforge.network.S2CMoonStateSync;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Luna Oscura/Roja: evento mundial escalable.
 *
 * onServerTick vigila el reloj del Overworld cada ~10s (throttle por
 * tickCount, igual que el resto del mod usa tickCount%N para entidades) y
 * decide cuando empieza/termina la Luna. onFinalizeSpawn es el gancho que
 * realmente equipa a los mobs hostiles — se dispara justo despues de que
 * vainilla decide el spawn (reemplazo moderno del viejo
 * MobSpawnEvent.FinalizeSpawn), asi que no hace falta ningun mixin nuevo.
 *
 * El estado (ronda, si la Luna esta activa ahora mismo) vive en
 * ArcaneMoonData, guardado a nivel de SERVIDOR (no por dimension, no por
 * jugador) — un unico contador compartido, igual en un mundo de 1 jugador
 * que en uno de 10.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneMoonHandler {

    private static final long DAY_TICKS = 24000L;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;

        ArcaneMoonData data = ArcaneMoonData.get(overworld);
        long tick = server.getTickCount();

        if (!ArcaneServerConfig.get(overworld).isMoonEventEnabled()) {
            if (data.isMoonActive() && tick % 200 == 0) {
                data.endMoon();
                syncMoonState(server, false);
            }
            return; // sistema apagado por /arcaneforge moon disable: ni caza, ni horda, ni nuevas rondas
        }

        if (data.isMoonActive()) {
            if (tick % Config.MOON_HUNT_RETARGET_TICKS == 0) {
                huntNearbyMonsters(overworld);
            }
            if (tick % Config.MOON_HORDE_INTERVAL_TICKS == 0) {
                spawnHordeWaves(overworld, data);
            }
        }

        if (tick % 200 != 0) return; // el chequeo de dia/ronda solo hace falta revisarlo cada ~10s

        long gameTime = overworld.getGameTime();

        if (data.isMoonActive()) {
            if (gameTime >= data.getMoonEndGameTime()) {
                data.endMoon();
                broadcast(server, "§7La Luna Oscura se retira con el amanecer... por ahora.");
                showTitle(server, "§7§lLA LUNA SE RETIRA", "§7Vuelve la calma... por ahora", 10, 40, 20);
                syncMoonState(server, false);
            }
            return;
        }

        long intervalTicks = Config.MOON_INTERVAL_DAYS * DAY_TICKS;
        long lastMoon = data.getLastMoonGameTime() < 0 ? 0L : data.getLastMoonGameTime();
        if (gameTime - lastMoon < intervalTicks) return;

        data.startMoon(gameTime, gameTime + Config.MOON_DURATION_TICKS);

        broadcast(server, "§4§l¡La Luna Oscura se alza! §rRonda " + data.getRound()
                + " — los mobs de esta noche son mucho mas peligrosos.");
        showTitle(server, "§4§l☾ LA LUNA OSCURA SE ALZA ☾", "§7Ronda " + data.getRound() + " — ten cuidado esta noche", 10, 70, 20);
        syncMoonState(server, true);
        for (var player : server.getPlayerList().getPlayers()) {
            overworld.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 0.6f, 0.8f);
        }
    }

    /** Refuerza cada pocos ticks que TODO mob hostil dentro del radio de caza persiga al jugador — no solo los recien spawneados. */
    private static void huntNearbyMonsters(ServerLevel overworld) {
        for (ServerPlayer player : overworld.players()) {
            List<Monster> nearby = overworld.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(ArcaneMoonLogic.HUNT_RADIUS), Monster::isAlive);
            for (Monster monster : nearby) {
                monster.setTarget(player);
            }
        }
    }

    /** Aparece una oleada de mobs junto a cada jugador — ver ArcaneMoonLogic.spawnHordeWave. */
    private static void spawnHordeWaves(ServerLevel overworld, ArcaneMoonData data) {
        for (ServerPlayer player : overworld.players()) {
            ArcaneMoonLogic.spawnHordeWave(player, overworld, overworld.getRandom(), data);
        }
    }

    /** Avisa a todos los jugadores conectados si la Luna Roja esta activa, para que el cliente tiña el cielo. */
    public static void syncMoonState(MinecraftServer server, boolean active) {
        S2CMoonStateSync pkt = new S2CMoonStateSync(active);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, pkt);
        }
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    /** Titulo grande centrado en pantalla, estilo aviso de oleada de servidores tipo Karmaland/Bedrock. */
    private static void showTitle(MinecraftServer server, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;

        ServerLevel serverLevel = event.getLevel().getLevel();
        if (serverLevel.dimension() != Level.OVERWORLD) return; // v1: solo overworld

        if (!ArcaneServerConfig.get(serverLevel).isMoonEventEnabled()) return;

        ArcaneMoonData data = ArcaneMoonData.get(serverLevel);
        ArcaneMoonLogic.equipMob(monster, serverLevel.getRandom(), data, serverLevel);
    }
}
