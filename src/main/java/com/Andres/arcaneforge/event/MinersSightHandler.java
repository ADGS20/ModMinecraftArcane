package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.miners.MinersSightLogic;
import com.Andres.arcaneforge.miners.OreFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VISION MINERA — mientras esta activada, resalta a traves de las paredes los
 * bloques de mineral cercanos que coincidan con el filtro elegido en el
 * casco. Cada mineral se marca con una entidad "block display" (la misma que
 * usa /summon minecraft:block_display) puesta exactamente encima del bloque
 * real, mostrando el CUBO tal cual en vez de una silueta rara — y con
 * Entity.setGlowingTag(true) se ve el contorno a traves de las paredes,
 * coloreado via equipo de scoreboard segun el mineral. Cuesta experiencia
 * cada ciclo de escaneo mientras esta activa; si el jugador se queda sin XP,
 * se apaga sola.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class MinersSightHandler {

    private static final int SCAN_INTERVAL_TICKS = 40; // cada 2s
    private static final int MAX_MARKERS = 48;
    private static final String TEAM_PREFIX = "arcaneforge_ore_";

    private static final Map<UUID, List<Display.BlockDisplay>> MARKERS = new HashMap<>();
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
        List<Display.BlockDisplay> newMarkers = new ArrayList<>();
        for (BlockPos pos : matches) {
            BlockState state = serverLevel.getBlockState(pos);
            OreFilter classified = OreFilter.classify(state.getBlock());
            if (classified == null) continue;
            PlayerTeam team = teamFor(scoreboard, classified);

            Display.BlockDisplay marker = createBlockMarker(serverLevel, pos, state);
            if (marker == null) continue;

            serverLevel.addFreshEntity(marker);
            scoreboard.addPlayerToTeam(marker.getScoreboardName(), team);
            newMarkers.add(marker);
        }
        if (!newMarkers.isEmpty()) MARKERS.put(sp.getUUID(), newMarkers);
        ArcaneForge.LOGGER.info("[MINERS-SCAN] marcadores creados={}", newMarkers.size());
    }

    /** Cache de los setters privados, resueltos una sola vez. */
    private static Method setBlockStateMethod;
    private static Method setBrightnessOverrideMethod;

    /**
     * Crea un "block display" (la misma entidad de /summon minecraft:block_display)
     * en la posicion del bloque, mostrando el CUBO real del mineral en vez de
     * una figura humanoide. El estado del bloque se fija llamando DIRECTO al
     * setter privado por reflexion en vez de pasar por un round-trip
     * NBT/Codec (Entity.load(ValueInput) con "block_state"): ese camino
     * compilaba y no lanzaba ningun error, pero el bloque salia invisible
     * (solo se veia el contorno del brillo) — probablemente el codec no
     * decodificaba bien el estado y caia en silencio a Blocks.AIR. Llamando
     * al setter tal cual lo hace el propio juego internamente nos ahorramos
     * esa serializacion/deserializacion completa.
     *
     * Ademas se fuerza Brightness.FULL_BRIGHT (tambien via reflexion, mismo
     * motivo): el marcador se coloca en la posicion REAL del mineral, que
     * casi siempre esta dentro de roca sin luz, y el render usa la luz de
     * esa posicion para sombrear el modelo — sin esto el cubo sale negro
     * solido (se ve, pero no se distingue nada) en vez de mostrarse
     * iluminado como corresponde a un indicador de "vision a traves de
     * paredes".
     */
    private static Display.BlockDisplay createBlockMarker(ServerLevel level, BlockPos pos, BlockState state) {
        Display.BlockDisplay marker = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        marker.setPos(pos.getX(), pos.getY(), pos.getZ());
        marker.setInvulnerable(true);
        marker.setSilent(true);
        marker.setNoGravity(true);
        marker.setGlowingTag(true);

        try {
            if (setBlockStateMethod == null) {
                setBlockStateMethod = Display.BlockDisplay.class.getDeclaredMethod("setBlockState", BlockState.class);
                setBlockStateMethod.setAccessible(true);
            }
            setBlockStateMethod.invoke(marker, state);

            if (setBrightnessOverrideMethod == null) {
                setBrightnessOverrideMethod = Display.class.getDeclaredMethod("setBrightnessOverride", Brightness.class);
                setBrightnessOverrideMethod.setAccessible(true);
            }
            setBrightnessOverrideMethod.invoke(marker, Brightness.FULL_BRIGHT);
        } catch (ReflectiveOperationException e) {
            ArcaneForge.LOGGER.warn("[MINERS-SCAN] no se pudo configurar el marcador via reflexion: {}", e.toString());
            return null;
        }

        return marker;
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
        List<Display.BlockDisplay> list = MARKERS.remove(sp.getUUID());
        if (list == null) return;
        for (Display.BlockDisplay d : list) {
            if (d.isAlive()) d.discard();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) clearMarkers(sp);
    }
}
