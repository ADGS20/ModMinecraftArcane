package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Comportamiento COMPARTIDO por cualquier Golem Arcano (de hierro o de
 * nieve) fuera del combate — lo especifico de cada tipo (inmunidad
 * ambiental al fuego/ahogo, daño de las bolas de nieve, flotar en lava)
 * vive en ArcaneSnowGolemHandler:
 *  - NINGUN Golem Arcano se hunde en agua (camina sobre la superficie, mismo
 *    truco que Paso Infernal para la lava en LavaWalkHandler, pero sin
 *    dañar/quemar nada porque el agua no hace nada malo, solo hay que
 *    evitar el hundido) — antes solo aplicaba al de Hierro; el de Nieve
 *    tambien lo necesita (su propia inmunidad al ahogo solo evita el daño,
 *    no evita que camine hundido).
 *  - Se auto-repara con cada enemigo que mata: cura tanta vida como XP
 *    valdria ese enemigo (getExperienceReward ya existe en vanilla y es
 *    publico, asi que no hace falta inventar una formula desde cero).
 *  - Aura de particulas continua ("estilo de encantamiento"): no podemos
 *    recolorear el modelo del golem sin un renderer/render-state propio
 *    (mucho riesgo para poco beneficio), asi que en su lugar el golem
 *    suelta chispas moradas constantemente mientras esta activo, mas densas
 *    cuanto mas nivel total tenga vinculado — el mismo lenguaje visual que
 *    ya usan Vampiro, Paso Infernal y el propio Cetro al vincular.
 *  - Lealtad (vainilla, la del tridente) vinculada con el Cetro: el golem
 *    sigue a quien lo vinculo como un perro. No tocamos el goalSelector del
 *    Mob (es protected, no hay forma limpia de agregarle un Goal desde
 *    fuera sin un mixin arriesgado), asi que en vez de eso se le da un
 *    empujon de pathfinding manual con getNavigation().moveTo(...) cada
 *    segundo mientras no este peleando con nada — mismo patron que ya usan
 *    Carga Rapida y el no-hundirse-en-lava del Golem de Nieve. Por eso el
 *    tamaño se reduce a la mitad cuando lleva Lealtad (ver
 *    GolemBindingRod/SnowGolemBindingRod): uno grande no cabe siguiendo al
 *    jugador por huecos de 2 bloques.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneGolemHandler {

    @SubscribeEvent
    public static void onGolemTick(EntityTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof AbstractGolem golem)) return;
            if (!(golem.level() instanceof ServerLevel serverLevel)) return;
            if (!ArcaneGolemUtil.isArcaneGolem(golem)) return;

            onGolemAura(golem, serverLevel);
            onGolemNoSink(golem);
            onGolemFollowOwner(golem, serverLevel);
            onGolemHuntCreeper(golem, serverLevel);
            onGolemNoFriendlyTarget(golem);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneGolem tick error: {}", e.getMessage());
        }
    }

    private static final double FOLLOW_STOP_DISTANCE_SQR = 3.0 * 3.0;

    /** Con Lealtad vinculada, el golem persigue a quien lo vinculo (como un perro) cuando no esta peleando. */
    private static void onGolemFollowOwner(AbstractGolem golem, ServerLevel serverLevel) {
        if (!ArcaneGolemUtil.isLoyal(golem)) return;
        if (golem.getTarget() != null) return;
        if (golem.tickCount % 20 != 0) return;

        UUID ownerId = ArcaneGolemUtil.getOwner(golem);
        if (ownerId == null) return;
        Player owner = serverLevel.getPlayerByUUID(ownerId);
        if (owner == null || !owner.isAlive()) return;

        if (golem.distanceToSqr(owner) > FOLLOW_STOP_DISTANCE_SQR) {
            golem.getNavigation().moveTo(owner, 1.0);
        } else {
            golem.getNavigation().stop();
        }
    }

    private static final double CREEPER_HUNT_RADIUS = 16.0;

    /**
     * Vainilla excluye a proposito al Creeper de los objetivos del gólem de
     * hierro (para que no se autodetone peleando cerca de uno) — ver el
     * predicado de NearestAttackableTargetGoal en IronGolem.registerGoals y
     * el propio canAttack(). Un Golem Arcano puede llevar Coraza Arcana /
     * Proteccion del Vacio encantadas y se auto-repara al matar (ver
     * onGolemKillAutoRepair), asi que ese riesgo deja de tener sentido — lo
     * anulamos con ArcaneGolemCreeperTargetMixin (canAttack) y aqui le damos
     * el empujon manual para que SI lo busque como objetivo (el goal
     * selector de vainilla nunca lo va a proponer por su cuenta).
     */
    private static void onGolemHuntCreeper(AbstractGolem golem, ServerLevel serverLevel) {
        if (golem.getTarget() != null) return;
        if (golem.tickCount % 20 != 0) return;

        List<Creeper> nearby = serverLevel.getEntitiesOfClass(Creeper.class,
                golem.getBoundingBox().inflate(CREEPER_HUNT_RADIUS), Creeper::isAlive);
        Creeper closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (Creeper creeper : nearby) {
            double distSqr = golem.distanceToSqr(creeper);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closest = creeper;
            }
        }
        if (closest != null) {
            golem.setTarget(closest);
        }
    }

    /**
     * onGolemFriendlyFire pone el daño en 0, pero eso no basta: vainilla
     * registra "quien me golpeo por ultimo" (setLastHurtByMob) ANTES de que
     * el Pre event pueda intervenir (mismo orden ya documentado en
     * SnowGolemImmunityMixin para animacion/sonido/empuje), y el goal
     * HurtByTargetGoal lee ese dato para poner como objetivo a quien golpeo,
     * sin importar que el daño real terminara siendo 0. Por eso un roce
     * accidental (una bola de nieve, un golpe de Cadena Arcana rebotado)
     * igual dejaba a los golems peleandose entre si indefinidamente. Aqui se
     * corta ese objetivo apenas aparece, todos los ticks.
     */
    private static void onGolemNoFriendlyTarget(AbstractGolem golem) {
        LivingEntity target = golem.getTarget();
        if (target instanceof AbstractGolem targetGolem && ArcaneGolemUtil.isArcaneGolem(targetGolem)) {
            golem.setTarget(null);
        }
    }

    /**
     * Dos Golems Arcanos nunca deben pelear entre ellos por daño accidental
     * (una bola de nieve que golpea de paso a otro golem, un salto de Cadena
     * Arcana que rebota hacia un aliado, etc.) — sin esto, ese roce accidental
     * hace que se ataquen entre si indefinidamente. Prioridad LOWEST para que
     * corra despues de cualquier otro handler que modifique el daño (mismo
     * patron que usa Coraza Arcana), y asi siempre tenga la ultima palabra.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGolemFriendlyFire(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (!(target instanceof AbstractGolem golemTarget) || !ArcaneGolemUtil.isArcaneGolem(golemTarget)) return;

            Entity attacker = event.getSource().getEntity();
            if (!(attacker instanceof AbstractGolem golemAttacker) || !ArcaneGolemUtil.isArcaneGolem(golemAttacker)) return;

            event.setNewDamage(0);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneGolem friendly-fire guard error: {}", e.getMessage());
        }
    }

    private static void onGolemAura(AbstractGolem golem, ServerLevel serverLevel) {
        if (golem.tickCount % 10 != 0) return;

        int totalLevels = ArcaneGolemUtil.totalBoundLevels(golem);
        int particles = Math.min(1 + totalLevels / 3, 6);

        serverLevel.sendParticles(new DustParticleOptions(0x9400D3, 1.2f),
                golem.getX(), golem.getY() + golem.getBbHeight() * 0.6, golem.getZ(),
                particles, golem.getBbWidth() * 0.35, golem.getBbHeight() * 0.35, golem.getBbWidth() * 0.35, 0.0);
    }

    private static void onGolemNoSink(AbstractGolem golem) {
        if (!golem.isInWater()) return;

        BlockPos check = golem.blockPosition();
        for (int i = 0; i < 3 && !golem.level().getFluidState(check).is(FluidTags.WATER); i++) {
            check = check.below();
        }
        double surfaceY = check.getY() + 1.0;

        if (golem.getY() < surfaceY) {
            golem.setPos(golem.getX(), surfaceY, golem.getZ());
            Vec3 mov = golem.getDeltaMovement();
            golem.setDeltaMovement(mov.x, Math.max(mov.y, 0.0), mov.z);
            golem.fallDistance = 0.0f;
        }
    }

    @SubscribeEvent
    public static void onGolemKillAutoRepair(LivingDeathEvent event) {
        try {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return;
            if (victim instanceof AbstractGolem) return;

            Entity killer = event.getSource().getEntity();
            if (!(killer instanceof AbstractGolem golem) || !(golem.level() instanceof ServerLevel serverLevel)) return;
            if (!ArcaneGolemUtil.isArcaneGolem(golem)) return;

            int xp = victim.getExperienceReward(serverLevel, golem);
            if (xp > 0) {
                golem.heal(xp);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneGolem auto-repair error: {}", e.getMessage());
        }
    }
}
