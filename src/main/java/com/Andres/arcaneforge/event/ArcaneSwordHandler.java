package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Los 8 encantamientos nuevos de espada. Todos comparten el mismo evento
 * (LivingDamageEvent.Pre) y el mismo item (espada, via tag "sweeping" que en
 * vanilla solo contiene espadas).
 *
 * RENDIMIENTO — un solo @SubscribeEvent, no nueve: antes cada uno de los 8
 * encantamientos (9 metodos, Marca del Cazador usa dos) tenia su PROPIO
 * listener independiente. LivingDamageEvent.Pre se dispara por CUALQUIER
 * golpe de CUALQUIER entidad viva del mundo (granjas de mobs, raids...), y
 * cada listener repetia por su cuenta: comprobar isClientSide, castear el
 * atacante, comprobar que el arma es espada, Y — lo mas caro — volver a leer
 * enteros el registro de encantamientos y el componente ENCHANTMENTS del
 * ItemStack desde cero. Con 9 listeners eso son 9 lecturas identicas del
 * mismo stack por cada golpe. Aqui se hace UNA sola vez (registro + lectura
 * de ItemEnchantments) y se resuelven los 8 niveles a partir de esa unica
 * lectura, antes de repartir el trabajo a los metodos applyXxx() de abajo.
 *
 * ORDEN DE EJECUCION preservado exactamente igual al de antes (importa: Corte
 * del Vacio pone un "piso" de daño que Marca del Cazador despues multiplica):
 * antes NeoForge ordenaba por prioridad — NORMAL primero (Cadena, Filo
 * Eterno, Filo Insaciable, Golpe Dimensional, Golpe Sismico, Sangria
 * Espectral, en ese orden de declaracion), luego LOW (Corte del Vacio,
 * despues el chequeo de Marca), luego LOWEST (decidir si aplicar marca
 * nueva). Aqui se reproduce ese mismo orden con llamadas secuenciales.
 *
 * IMPORTANTE — nivel sin techo real: la Arcane Forge permite subir CUALQUIER
 * encantamiento hasta nivel 255 con Pedestal (ver ArcaneForgeBlockEntity),
 * muy por encima del max_level=3 que declaran los JSON de estos encantos.
 * TODAS las formulas de aqui son ahora "de bandera": usan el nivel real sin
 * topar la entrada, y cada una pone su PROPIO Math.min en el resultado (no
 * en el nivel) para seguir dando algo hasta bien entrado el rango alto en
 * vez de aplanarse en nivel 3 como antes (ver ArcaneForgeBlockEntity.
 * getRealMaxLevel para el tope real que la Forja deja alcanzar a cada una).
 *
 * IMPORTANTE — reentrada: Cadena Arcana golpea a otros enemigos llamando a
 * hurt() dentro de este mismo evento. Eso dispara un LivingDamageEvent.Pre
 * NUEVO para cada golpe en cadena, que este mismo listener volveria a
 * procesar (podria encadenar de nuevo, teletransportar de nuevo, marcar de
 * nuevo...). IN_CHAIN evita que se procesen golpes que no vengan directo del
 * jugador — salvo el chequeo de Marca del Cazador (onHunterMarkCheck antes),
 * que SI debe aplicarse tambien a los golpes en cadena para que el
 * multiplicador de marca les afecte igual.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneSwordHandler {

    private static final Random RNG = new Random();

    /** true mientras se estan aplicando los golpes secundarios de Cadena Arcana. */
    private static boolean IN_CHAIN = false;

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, name));
    }

    private static final ResourceKey<Enchantment> CADENA_ARCANA      = key("cadena_arcana");
    private static final ResourceKey<Enchantment> CORTE_DEL_VACIO    = key("corte_del_vacio");
    private static final ResourceKey<Enchantment> GOLPE_DIMENSIONAL  = key("golpe_dimensional");
    private static final ResourceKey<Enchantment> FILO_ETERNO        = key("filo_eterno");
    private static final ResourceKey<Enchantment> MARCA_DEL_CAZADOR  = key("marca_del_cazador");
    private static final ResourceKey<Enchantment> GOLPE_SISMICO      = key("golpe_sismico");
    private static final ResourceKey<Enchantment> SANGRIA_ESPECTRAL  = key("sangria_espectral");
    private static final ResourceKey<Enchantment> FILO_INSACIABLE    = key("filo_insaciable");

    private static int levelOf(HolderLookup.RegistryLookup<Enchantment> registry, ItemEnchantments enchants, ResourceKey<Enchantment> key) {
        try {
            Optional<Holder.Reference<Enchantment>> opt = registry.get(key);
            if (opt.isEmpty()) return 0;
            return enchants.getLevel(opt.get());
        } catch (Exception e) { return 0; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUNTO DE ENTRADA UNICO — reemplaza los 9 @SubscribeEvent que habia antes.
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onSwordEnchants(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ItemTags.SWORDS)) return;

        Level level = attacker.level();

        HolderLookup.RegistryLookup<Enchantment> registry;
        ItemEnchantments enchants;
        try {
            registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            enchants = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        } catch (Exception e) {
            return; // sin registro/componentes no hay nada que hacer para ninguno de los 8
        }

        // Todos "de bandera" ahora: nivel real sin topar aqui, cada applyXxx
        // pone su propio techo en el resultado (ver getRealMaxLevel).
        int lvlCadena       = levelOf(registry, enchants, CADENA_ARCANA);
        int lvlFiloEterno   = levelOf(registry, enchants, FILO_ETERNO);
        int lvlFiloInsac    = levelOf(registry, enchants, FILO_INSACIABLE);
        int lvlCorteVacio   = levelOf(registry, enchants, CORTE_DEL_VACIO);
        int lvlMarca        = levelOf(registry, enchants, MARCA_DEL_CAZADOR);
        int lvlGolpeDim     = levelOf(registry, enchants, GOLPE_DIMENSIONAL);
        int lvlGolpeSism    = levelOf(registry, enchants, GOLPE_SISMICO);
        int lvlSangria      = levelOf(registry, enchants, SANGRIA_ESPECTRAL);

        // ── Orden NORMAL (igual que la declaracion original) ──
        if (!IN_CHAIN) {
            if (lvlCadena > 0 && level instanceof ServerLevel serverLevel) {
                applyCadenaArcana(event, target, attacker, serverLevel, lvlCadena);
            }
            if (lvlFiloEterno > 0) applyFiloEterno(event, target, attacker, lvlFiloEterno);
            if (lvlFiloInsac > 0) applyFiloInsaciable(event, target, lvlFiloInsac);
            if (lvlGolpeDim > 0 && level instanceof ServerLevel serverLevel) {
                applyGolpeDimensional(target, attacker, serverLevel, lvlGolpeDim);
            }
            if (lvlGolpeSism > 0 && level instanceof ServerLevel serverLevel) {
                applyGolpeSismico(target, attacker, serverLevel, lvlGolpeSism);
            }
            if (lvlSangria > 0) applySangriaEspectral(target, lvlSangria);
        }

        // ── Orden LOW ──
        if (!IN_CHAIN && lvlCorteVacio > 0) {
            applyCorteDelVacio(event, lvlCorteVacio);
        }
        applyHunterMarkCheck(event, target); // SIN guard IN_CHAIN: afecta tambien golpes en cadena

        // ── Orden LOWEST ──
        if (!IN_CHAIN && lvlMarca > 0) {
            applyHunterMarkApply(target, lvlMarca);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. CADENA ARCANA — daño en cadena a enemigos cercanos
    // ═══════════════════════════════════════════════════════════════════════════

    private static void applyCadenaArcana(LivingDamageEvent.Pre event, LivingEntity target, LivingEntity livingAttacker, ServerLevel serverLevel, int level) {
        try {
            // Techos re-escalados: a nivel 3 da lo mismo que antes (3 objetivos,
            // 65%, radio 6), pero ahora sigue creciendo con mas inversion en
            // vez de aplanarse ahi.
            int extraTargets = Math.min(level, 10);            // hasta 10 objetivos en cadena
            float fraction = Math.min(0.35f + 0.10f * level, 0.90f); // hasta 90%
            double radius = Math.min(3.0 + level, 40.0);       // hasta 40 bloques

            float chainDamage = event.getOriginalDamage() * fraction;
            if (chainDamage <= 0) return;

            // Cast seguro: un mob hostil tambien puede empuñar una espada con
            // Cadena Arcana (los zombis recogen y usan espadas en vainilla), asi
            // que el atacante no siempre es un Player — antes esto tiraba
            // ClassCastException para cualquier no-jugador, silenciado por el
            // catch de abajo (la cadena simplemente no se aplicaba, sin pista
            // de por que).
            DamageSource chainSource = (livingAttacker instanceof Player player)
                    ? serverLevel.damageSources().playerAttack(player)
                    : serverLevel.damageSources().mobAttack(livingAttacker);

            AABB area = target.getBoundingBox().inflate(radius);
            int hit = 0;
            IN_CHAIN = true;
            try {
                for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (hit >= extraTargets) break;
                    if (nearby == target || nearby == livingAttacker || !nearby.isAlive()) continue;
                    nearby.hurt(chainSource, chainDamage);
                    hit++;
                }
            } finally {
                IN_CHAIN = false;
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("CadenaArcana error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. FILO ETERNO — combo: golpes consecutivos al mismo objetivo suman daño
    // ═══════════════════════════════════════════════════════════════════════════

    private record ComboState(UUID target, int count, long lastHitTick) {}
    private static final Map<UUID, ComboState> COMBOS = new HashMap<>();
    private static final long COMBO_WINDOW_TICKS = 60L; // 3s

    private static void applyFiloEterno(LivingDamageEvent.Pre event, LivingEntity target, LivingEntity livingAttacker, int level) {
        try {
            long now = livingAttacker.level().getGameTime();
            // A nivel 3 sigue dando cap=11 (igual que antes); techo re-escalado
            // a 60 golpes de combo para que invertir mas siga dando algo.
            int cap = Math.min(5 + level * 2, 60);

            ComboState prev = COMBOS.get(livingAttacker.getUUID());
            int count;
            if (prev != null && prev.target().equals(target.getUUID()) && (now - prev.lastHitTick()) <= COMBO_WINDOW_TICKS) {
                count = Math.min(prev.count() + 1, cap);
            } else {
                count = 1;
            }
            COMBOS.put(livingAttacker.getUUID(), new ComboState(target.getUUID(), count, now));

            if (count > 1) {
                float bonus = event.getOriginalDamage() * 0.08f * (count - 1);
                event.setNewDamage(event.getNewDamage() + bonus);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("FiloEterno error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. FILO INSACIABLE — mas daño cuanta menos vida le queda al objetivo
    // ═══════════════════════════════════════════════════════════════════════════

    private static void applyFiloInsaciable(LivingDamageEvent.Pre event, LivingEntity target, int level) {
        try {
            if (target.getMaxHealth() <= 0) return;

            float missingFraction = 1.0f - (target.getHealth() / target.getMaxHealth());
            missingFraction = Math.max(0.0f, Math.min(1.0f, missingFraction));

            // Tambien "de bandera": escala suave hasta el tope real de 250.
            float scale = Math.min(0.05f + 0.0024f * level, 0.65f);
            float bonus = event.getOriginalDamage() * missingFraction * scale;
            if (bonus > 0) {
                event.setNewDamage(event.getNewDamage() + bonus);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("FiloInsaciable error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. CORTE DEL VACÍO — probabilidad de ignorar parte de la armadura
    // ═══════════════════════════════════════════════════════════════════════════

    private static void applyCorteDelVacio(LivingDamageEvent.Pre event, int level) {
        try {
            // Corte del Vacío es "de bandera" (multiplicador 5x en la Forja):
            // escala suave hasta el tope real de 250, no se aplana en nivel 3.
            float chance = Math.min(0.10f + 0.0026f * level, 0.75f);
            if (RNG.nextFloat() >= chance) return;

            float floor = event.getOriginalDamage() * Math.min(0.40f + 0.0022f * level, 0.95f);
            if (event.getNewDamage() < floor) {
                event.setNewDamage(floor);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("CorteDelVacio error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. MARCA DEL CAZADOR — objetivo marcado recibe mas daño de cualquiera
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String MARK_TAG = "arcaneforge_hunter_mark_expiry";
    private static final String MARK_LEVEL_TAG = "arcaneforge_hunter_mark_level";

    /** Revisa la marca ANTES de que se apliquen nuevas (para que un golpe no se auto-bonifique). */
    private static void applyHunterMarkCheck(LivingDamageEvent.Pre event, LivingEntity target) {
        try {
            var data = target.getPersistentData();
            if (!data.contains(MARK_TAG)) return;

            long expiry = data.getLong(MARK_TAG).orElse(0L);
            if (target.level().getGameTime() >= expiry) {
                data.remove(MARK_TAG);
                data.remove(MARK_LEVEL_TAG);
                return;
            }

            int markLevel = data.getInt(MARK_LEVEL_TAG).orElse(1);
            float multiplier = 1.0f + Math.min(0.10f * markLevel, 1.0f); // hasta +100%
            event.setNewDamage(event.getNewDamage() * multiplier);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("HunterMarkCheck error: {}", e.getMessage());
        }
    }

    /** Decide si ESTE golpe deja marcado al objetivo para los siguientes. */
    private static void applyHunterMarkApply(LivingEntity target, int level) {
        try {
            float chance = Math.min(0.25f * level, 0.90f); // hasta 90%
            if (RNG.nextFloat() >= chance) return;

            long duration = Math.min(100L + 20L * level, 600L); // hasta 30s
            var data = target.getPersistentData();
            data.putLong(MARK_TAG, target.level().getGameTime() + duration);
            data.putInt(MARK_LEVEL_TAG, level);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("HunterMarkApply error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. GOLPE DIMENSIONAL — teletransporte corto detras del objetivo
    // ═══════════════════════════════════════════════════════════════════════════

    private static final Map<UUID, Long> DIMENSIONAL_COOLDOWN = new HashMap<>();
    private static final long DIMENSIONAL_COOLDOWN_TICKS = 40L; // 2s

    private static void applyGolpeDimensional(LivingEntity target, LivingEntity livingAttacker, ServerLevel serverLevel, int level) {
        try {
            long now = livingAttacker.level().getGameTime();
            // El cooldown tambien se acorta con el nivel (40 ticks a nivel bajo,
            // hasta 10 ticks/0.5s en inversion alta), asi que subir de nivel
            // sigue dando algo incluso despues de topar la probabilidad.
            long cooldownTicks = Math.max(10L, DIMENSIONAL_COOLDOWN_TICKS - level / 5);
            Long last = DIMENSIONAL_COOLDOWN.get(livingAttacker.getUUID());
            if (last != null && (now - last) < cooldownTicks) return;

            float chance = Math.min(0.20f * level, 0.85f); // hasta 85%
            if (RNG.nextFloat() >= chance) return;

            Vec3 behind = target.position().subtract(livingAttacker.position()).normalize().scale(1.2);
            double destX = target.getX() + behind.x;
            double destZ = target.getZ() + behind.z;
            double destY = target.getY();

            Level world = livingAttacker.level();
            var feetPos = net.minecraft.core.BlockPos.containing(destX, destY, destZ);
            // isSolid() en vez de .isAir() literal: bloquea solo si de verdad te
            // impide pisarlo/atravesarlo (pasto, flores, nieve, antorchas... no bloquean).
            if (world.getBlockState(feetPos).isSolid() || world.getBlockState(feetPos.above()).isSolid()) {
                return; // sin espacio seguro: se queda quieto, el golpe normal sigue
            }

            double eyeX = destX;
            double eyeY = destY + livingAttacker.getEyeHeight();
            double eyeZ = destZ;
            Vec3 targetEyes = target.getEyePosition();
            double xd = targetEyes.x - eyeX;
            double yd = targetEyes.y - eyeY;
            double zd = targetEyes.z - eyeZ;
            double horizDist = Math.sqrt(xd * xd + zd * zd);
            float newXRot = Mth.wrapDegrees((float) (-(Mth.atan2(yd, horizDist) * 180.0F / (float) Math.PI)));
            float newYRot = Mth.wrapDegrees((float) (Mth.atan2(zd, xd) * 180.0F / (float) Math.PI) - 90.0F);

            livingAttacker.teleportTo(serverLevel, destX, destY, destZ, Set.of(), newYRot, newXRot, true);
            livingAttacker.fallDistance = 0.0f;
            DIMENSIONAL_COOLDOWN.put(livingAttacker.getUUID(), now); // referencia: cooldownTicks calculado arriba

            serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x6A0DAD, 1.2f),
                    destX, destY + 1.0, destZ, 12, 0.2, 0.3, 0.2, 0.0);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("GolpeDimensional error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. GOLPE SÍSMICO — onda que aturde/empuja enemigos cercanos al objetivo
    // ═══════════════════════════════════════════════════════════════════════════

    private static void applyGolpeSismico(LivingEntity target, LivingEntity livingAttacker, ServerLevel serverLevel, int level) {
        try {
            float chance = Math.min(0.20f * level, 0.85f); // hasta 85%
            if (RNG.nextFloat() >= chance) return;

            double radius = Math.min(2.5 + level * 0.7, 20.0); // hasta 20 bloques
            Vec3 pos = target.position();
            AABB area = new AABB(pos.x - radius, pos.y - 1, pos.z - radius, pos.x + radius, pos.y + 3, pos.z + radius);
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                if (nearby == livingAttacker || nearby == target) continue;
                Vec3 dir = nearby.position().subtract(pos);
                double len = dir.length();
                Vec3 dirNorm = len > 0.001 ? dir.scale(1.0 / len) : new Vec3(0, 0, 1);
                nearby.push(dirNorm.x * 1.2, 0.35, dirNorm.z * 1.2);
                nearby.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 3, false, false, true));
            }

            serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xC48A2E, 1.5f),
                    pos.x, pos.y, pos.z, 20, radius * 0.4, 0.3, radius * 0.4, 0.0);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("GolpeSismico error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. SANGRÍA ESPECTRAL — sangrado que escala con golpes consecutivos
    // ═══════════════════════════════════════════════════════════════════════════

    private static void applySangriaEspectral(LivingEntity target, int level) {
        try {
            float chance = Math.min(0.30f * level, 0.90f); // 30/60/90%
            if (RNG.nextFloat() >= chance) return;

            MobEffectInstance current = target.getEffect(MobEffects.WITHER);
            int amplitude = current != null ? Math.min(current.getAmplifier() + 1, 4) : 0; // hasta nivel 5 de wither (amp 4)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, amplitude, false, true, true));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("SangriaEspectral error: {}", e.getMessage());
        }
    }
}
