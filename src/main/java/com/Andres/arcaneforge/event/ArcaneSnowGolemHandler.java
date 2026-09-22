package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comportamiento propio del Golem de Nieve Arcano (mismo Cetro que el Golem
 * de Hierro — ver GolemBindingRod/ArcaneGolemUtil — pero vinculado sobre un
 * SnowGolem en vez de un IronGolem):
 *  - Vence su propia debilidad ambiental una vez vinculado: ya no se derrite
 *    en biomas calidos (SnowGolem.aiStep aplica daño de fuego cada tick via
 *    EnvironmentAttributes.SNOW_GOLEM_MELTS) ni se lastima con la lluvia/agua
 *    (LivingEntity.isSensitiveToWater -> daño de ahogo cada tick) — el mismo
 *    "supera su debilidad natural" que ya tiene el Golem de Hierro no
 *    hundiendose en agua, pero aplicado a la debilidad que le toca a este.
 *  - Sus bolas de nieve pegan de verdad: vainilla les pone daño 0 contra casi
 *    todo (Snowball.onHitEntity solo hace algo especial contra Blaze/Ender
 *    Dragon), asi que un Golem de Nieve sin vincular es puramente decorativo
 *    en combate. Vinculado, cada bola inflige daño real escalado con la suma
 *    de niveles de encantamiento atados — el mismo totalBoundLevels que ya
 *    usa el Golem de Hierro para su daño cuerpo a cuerpo — para que sea la
 *    variante de rango/control frente al tanque cuerpo a cuerpo.
 *  - Ya no se hunde en lava: camina por encima de la superficie igual que
 *    el Golem de Hierro no se hunde en agua (ArcaneGolemHandler.
 *    onGolemNoSink) y que Paso Infernal hace flotar al jugador
 *    (LavaWalkHandler) — aqui es automatico por estar vinculado, sin
 *    necesitar ningun encantamiento puesto (la inmunidad al daño de fuego
 *    de arriba ya evitaba que se quemara, pero seguia hundiendose
 *    fisicamente si caminaba hacia adentro).
 *  - Carga Rapida (vainilla) vinculada con el Cetro: dispara bolas de nieve
 *    EXTRA cada vez mas seguido segun el nivel. En vainilla ese encanto solo
 *    acorta el tiempo de carga de una ballesta EN LA MANO — un golem no
 *    sostiene nada, asi que no hay "carga" que acortar de forma nativa. En
 *    vez de tocar la IA del mob (su RangedAttackGoal trae un intervalo fijo
 *    de 20 ticks metido en el constructor de vainilla, no hay forma limpia
 *    de editarlo sin un mixin de por si arriesgado), se traduce a disparos
 *    de bola de nieve ADICIONALES por encima del ritmo normal — mismo efecto
 *    percibido ("dispara mas rapido"), sin tocar la IA original.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneSnowGolemHandler {

    private static final ResourceKey<Enchantment> QUICK_CHARGE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("minecraft", "quick_charge"));

    /** Ultimo tick (por golem) en que se disparo un tiro EXTRA de Carga Rapida. */
    private static final Map<UUID, Long> LAST_BONUS_SHOT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onSnowGolemDamage(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;
            DamageSource source = event.getSource();

            // Inmunidad ambiental del propio golem de nieve vinculado.
            if (target instanceof SnowGolem snow && ArcaneGolemUtil.isArcaneGolem(snow)) {
                if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.DROWN)) {
                    event.setNewDamage(0);
                    return;
                }
            }

            // Bola de nieve disparada por un Golem de Nieve vinculado: daño real.
            // Techo re-escalado en la pasada "reward 255": la suma de niveles
            // vinculados ahora puede ser mucho mas alta que antes (un solo
            // encantamiento ya llega a 250), asi que el viejo tope de 15 se
            // alcanzaba con casi cualquier inversion seria y no premiaba nada
            // mas alla de eso.
            Entity direct = source.getDirectEntity();
            if (direct instanceof Snowball snowball
                    && snowball.getOwner() instanceof SnowGolem shooter
                    && ArcaneGolemUtil.isArcaneGolem(shooter)) {
                int totalLevels = ArcaneGolemUtil.totalBoundLevels(shooter);
                float extraDamage = Math.min(1.0f + 0.15f * totalLevels, 80.0f);
                event.setNewDamage(event.getNewDamage() + extraDamage);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneSnowGolem damage handler error: {}", e.getMessage());
        }
    }

    /**
     * Carga Rapida vinculada: mientras el golem tenga un objetivo vivo a la
     * vista y en rango (mismos 10 bloques que su RangedAttackGoal vainilla),
     * fuerza tiros de bola de nieve EXTRA con un intervalo que se acorta con
     * el nivel — de los 20 ticks (1s) normales hasta un piso de 2 ticks
     * (0.1s) en nivel 30 (ver ArcaneForgeBlockEntity.getRealMaxLevel
     * "quick_charge"). No reemplaza ni toca el ataque normal del golem, solo
     * le suma disparos encima.
     */
    @SubscribeEvent
    public static void onSnowGolemQuickCharge(EntityTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof SnowGolem golem)) return;
            if (golem.level().isClientSide()) return;
            if (!ArcaneGolemUtil.isArcaneGolem(golem)) return;

            onSnowGolemNoSinkLava(golem);

            int level = ArcaneGolemUtil.golemEnchantLevel(golem, QUICK_CHARGE_KEY);
            if (level <= 0) return;

            LivingEntity target = golem.getTarget();
            if (target == null || !target.isAlive()) return;
            if (golem.distanceToSqr(target) > 100.0) return; // 10 bloques, mismo radio que su RangedAttackGoal
            if (!golem.getSensing().hasLineOfSight(target)) return;

            long now = golem.level().getGameTime();
            long interval = Math.max(2L, Math.round(20.0 / (1.0 + 0.3 * level)));
            Long last = LAST_BONUS_SHOT.get(golem.getUUID());
            if (last != null && (now - last) < interval) return;

            golem.performRangedAttack(target, 1.0F);
            LAST_BONUS_SHOT.put(golem.getUUID(), now);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneSnowGolem quick-charge error: {}", e.getMessage());
        }
    }

    /** Flota justo encima de la superficie de la lava en vez de hundirse, igual que LavaWalkHandler para el jugador. */
    private static void onSnowGolemNoSinkLava(SnowGolem golem) {
        try {
            if (!golem.isInLava()) return;

            BlockPos check = golem.blockPosition();
            for (int i = 0; i < 2 && !golem.level().getFluidState(check).is(FluidTags.LAVA); i++) {
                check = check.below();
            }
            double surfaceY = check.getY() + 1.0;

            if (golem.getY() < surfaceY) {
                golem.setPos(golem.getX(), surfaceY, golem.getZ());
                Vec3 mov = golem.getDeltaMovement();
                golem.setDeltaMovement(mov.x, Math.max(mov.y, 0.0), mov.z);
                golem.fallDistance = 0.0f;
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneSnowGolem lava-walk error: {}", e.getMessage());
        }
    }
}
