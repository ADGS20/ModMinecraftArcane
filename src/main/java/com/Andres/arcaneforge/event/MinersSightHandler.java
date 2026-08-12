package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.miners.MinersSightLogic;
import com.Andres.arcaneforge.miners.OreFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VISION MINERA — mientras esta activada, resalta a traves de las paredes
 * (con el mismo mecanismo que el efecto Brillo de la flecha espectral: una
 * entidad marcador con Brillo permanente, coloreada via equipo de
 * scoreboard) los bloques de mineral cercanos que coincidan con el filtro
 * elegido en el casco. Cuesta experiencia cada ciclo de escaneo mientras
 * este activa; si el jugador se queda sin XP, se apaga sola.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class MinersSightHandler {

    private static final int SCAN_INTERVAL_TICKS = 40; // cada 2s
    private static final int MAX_MARKERS = 48;
    private static final String TEAM_PREFIX = "arcaneforge_ore_";

    private static final Map<UUID, List<ArmorStand>> MARKERS = new HashMap<>();
    /** Solo para no repetir el mismo log [MINERS-TICK] cada 2s sin razon. */
    private static final Map<UUID, Boolean> LAST_APPLICABLE = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            if (sp.tickCount % SCAN_INTERVAL_TICKS != 0) return;

            ItemStack helmet = sp.getItemBySlot(EquipmentSlot.HEAD);
            int level = MinersSightLogic.getLevel(sp, helmet);
            boolean enabled = MinersSightLogic.isEnabled(helmet);
            boolean applicable = sp.isAlive() && level > 0 && enabled;

            Boolean last = LAST_APPLICABLE.put(sp.getUUID(), applicable);
            if (level > 0 && !Boolean.valueOf(applicable).equals(last)) {
                ArcaneForge.LOGGER.info("[MINERS-TICK] helmet={} level={} enabled={} filter={} applicable={}",
                        helmet.getItem(), level, enabled, MinersSightLogic.getFilter(helmet).id(), applicable);
            }

            if (!applicable) {
                clearMarkers(sp);
                return;
            }

            if (!sp.isCreative()) {
                int xpCost = MinersSightLogic.xpCostForLevel(level);
                if (sp.experienceLevel <= 0 && sp.experienceProgress <= 0f) {
                    MinersSightLogic.setEnabled(helmet, false);
                    clearMarkers(sp);
                    sp.sendSystemMessage(Component.literal("§cVision Minera desactivada: sin experiencia"));
                    return;
                }
                sp.giveExperiencePoints(-xpCost);
            }

            scan(sp, helmet, level);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("MinersSight tick error: {}", e.getMessage());
        }
    }

    private static void scan(ServerPlayer sp, ItemStack helmet, int level) {
        if (!(sp.level() instanceof ServerLevel serverLevel)) return;

        OreFilter filter = MinersSightLogic.getFilter(helmet);
        int radius = MinersSightLogic.radiusForLevel(level);
        BlockPos center = sp.blockPosition();

        List<BlockPos> matches = new ArrayList<>();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mut.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!serverLevel.isLoaded(mut)) continue;
                    Block block = serverLevel.getBlockState(mut).getBlock();
                    OreFilter classified = OreFilter.classify(block);
                    if (classified == null) continue;
                    if (filter != OreFilter.ALL && classified != filter) continue;
                    matches.add(mut.immutable());
                }
            }
        }

        matches.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        if (matches.size() > MAX_MARKERS) matches = matches.subList(0, MAX_MARKERS);

        ArcaneForge.LOGGER.info("[MINERS-SCAN] center={} radius={} filter={} matches={}",
                center, radius, filter.id(), matches.size());

        // Cada escaneo se rehace desde cero: es mas simple y seguro que llevar
        // un diff incremental, y como el intervalo es de 2s no se nota parpadeo.
        clearMarkers(sp);

        Scoreboard scoreboard = serverLevel.getServer().getScoreboard();
        List<ArmorStand> newMarkers = new ArrayList<>();
        for (BlockPos pos : matches) {
            OreFilter classified = OreFilter.classify(serverLevel.getBlockState(pos).getBlock());
            if (classified == null) continue;
            PlayerTeam team = teamFor(scoreboard, classified);

            ArmorStand marker = new ArmorStand(serverLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            marker.setInvisible(true);
            marker.setNoGravity(true);
            marker.setInvulnerable(true);
            marker.setSilent(true);
            marker.setNoBasePlate(true);
            marker.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false, false));

            serverLevel.addFreshEntity(marker);
            scoreboard.addPlayerToTeam(marker.getScoreboardName(), team);
            newMarkers.add(marker);
        }
        if (!newMarkers.isEmpty()) MARKERS.put(sp.getUUID(), newMarkers);
        ArcaneForge.LOGGER.info("[MINERS-SCAN] marcadores creados={}", newMarkers.size());
    }

    private static PlayerTeam teamFor(Scoreboard scoreboard, OreFilter filter) {
        String name = TEAM_PREFIX + filter.id();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
            team.setColor(filter.color());
        }
        return team;
    }

    private static void clearMarkers(ServerPlayer sp) {
        List<ArmorStand> list = MARKERS.remove(sp.getUUID());
        if (list == null) return;
        for (ArmorStand a : list) {
            if (a.isAlive()) a.discard();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) clearMarkers(sp);
    }
}
