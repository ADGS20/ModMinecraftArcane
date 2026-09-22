package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.moon.ArcaneMoonLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * A partir de Config.MOON_HERALDO_DIG_MIN_ROUND, un Heraldo que no puede VER
 * a su objetivo (linea de vision bloqueada) intenta romper el terreno que lo
 * separa — asi un muro de tierra o unos arboles no bastan para escapar de un
 * Heraldo de ronda alta. Solo rompe bloques marcados en la tag de datapack
 * #arcaneforge:heraldo_diggable (terreno natural: piedra, tierra, arena,
 * troncos, hojas, etc.) — nunca algo fabricado por el jugador (tablones,
 * ladrillos, hormigon...), asi que una casa construida sigue siendo un
 * refugio real. Un servidor puede editar esa tag para ajustar que se puede
 * romper.
 *
 * Sin AI Goal nueva a proposito: el goalSelector de Mob es protected, no hay
 * forma limpia de agregarle un Goal desde fuera de la clase sin un mixin
 * (mismo motivo por el que ArcaneGolemHandler resuelve todo su comportamiento
 * "extra" via tick manual en vez de Goals).
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneHeraldoDigHandler {

    private static final TagKey<net.minecraft.world.level.block.Block> DIGGABLE = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "heraldo_diggable"));

    private static final double MAX_DIG_DISTANCE_SQR = 12.0 * 12.0;

    @SubscribeEvent
    public static void onMonsterTick(EntityTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof Monster monster)) return;
            if (!(monster.level() instanceof ServerLevel serverLevel)) return;
            if (monster.tickCount % Config.MOON_HERALDO_DIG_INTERVAL_TICKS != 0) return;

            if (ArcaneMoonLogic.heraldoRound(monster) < Config.MOON_HERALDO_DIG_MIN_ROUND) return;

            LivingEntity target = monster.getTarget();
            if (!(target instanceof Player)) return;
            if (monster.distanceToSqr(target) > MAX_DIG_DISTANCE_SQR) return;
            if (monster.getSensing().hasLineOfSight(target)) return; // ya lo ve, no hace falta cavar

            digTowardTarget(monster, serverLevel, target);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Heraldo dig error: {}", e.getMessage());
        }
    }

    private static void digTowardTarget(Monster monster, ServerLevel level, LivingEntity target) {
        Vec3 delta = target.position().subtract(monster.position());
        double horizontalLen = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalLen < 1.0e-4) return;

        int stepX = (int) Math.round(delta.x / horizontalLen);
        int stepZ = (int) Math.round(delta.z / horizontalLen);
        if (stepX == 0 && stepZ == 0) return;

        BlockPos base = monster.blockPosition();
        BlockPos step1 = base.offset(stepX, 0, stepZ);
        BlockPos step2 = step1.offset(stepX, 0, stepZ);

        boolean broke = tryBreak(level, monster, step1);
        broke |= tryBreak(level, monster, step1.above());
        broke |= tryBreak(level, monster, step2);
        broke |= tryBreak(level, monster, step2.above());

        if (broke) {
            level.playSound(null, monster.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.0f, 0.7f);
        }
    }

    private static boolean tryBreak(ServerLevel level, Monster monster, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.is(DIGGABLE)) return false;
        return level.destroyBlock(pos, false, monster);
    }
}
