package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Handler unificado para los nuevos encantamientos arcanos:
 *  - Ethereal Hook   (caña de pescar): drops raros garantizados
 *  - Soul Delve      (pala/herramienta): XP extra + área extendida
 *  - Arcane Cataclysm(mazo): AOE knockback + explosión
 *  - Void Wings      (élitro): sin daño de caída + durabilidad
 *  - Chain Thunder   (tridente): rayos en cadena a enemigos cercanos
 *  - Arcane Repulse  (escudo): daño reflejado + knockback al bloquear
 *  - Eternal Feast   (cualquier comida): hambre nunca baja + ruleta de efectos
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneEnchantsHandler {

    private static final java.util.Random RNG = new java.util.Random();

    // ── Claves de encantamiento ───────────────────────────────────────────────
    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(ArcaneForge.MODID, name));
    }

    private static final ResourceKey<Enchantment> ETHEREAL_HOOK    = key("ethereal_hook");
    private static final ResourceKey<Enchantment> SOUL_DELVE        = key("soul_delve");
    private static final ResourceKey<Enchantment> ARCANE_CATACLYSM  = key("arcane_cataclysm");
    private static final ResourceKey<Enchantment> VOID_WINGS        = key("void_wings");
    private static final ResourceKey<Enchantment> CHAIN_THUNDER     = key("chain_thunder");
    private static final ResourceKey<Enchantment> ARCANE_REPULSE    = key("arcane_repulse");
    private static final ResourceKey<Enchantment> ETERNAL_FEAST     = key("eternal_feast");
    private static final ResourceKey<Enchantment> CORAZA_ARCANA     = key("coraza_arcana");

    // ── Utilidad ──────────────────────────────────────────────────────────────
    private static int lvl(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(key);
            if (opt.isEmpty()) return 0;
            ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get());
        } catch (Exception e) { return 0; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. ETHEREAL HOOK — Caña de pescar con drops épicos
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        try {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            ItemStack rod = player.getMainHandItem();
            int level = lvl(player.level(), rod, ETHEREAL_HOOK);
            if (level <= 0) return;

            List<ItemStack> bonusDrops = new ArrayList<>();
            int rolls = Math.min(level, 30); // techo re-escalado, antes 10
            for (int i = 0; i < rolls; i++) {
                int roll = RNG.nextInt(100);
                if (roll < 5)            bonusDrops.add(new ItemStack(Items.NETHERITE_INGOT));
                else if (roll < 15)      bonusDrops.add(new ItemStack(Items.DIAMOND, 1 + RNG.nextInt(3)));
                else if (roll < 30)      bonusDrops.add(new ItemStack(Items.EMERALD, 2 + RNG.nextInt(4)));
                else if (roll < 50)      bonusDrops.add(new ItemStack(Items.ENDER_PEARL, 1 + RNG.nextInt(3)));
                else if (roll < 65)      bonusDrops.add(new ItemStack(Items.BLAZE_ROD, 1 + RNG.nextInt(4)));
                else                     bonusDrops.add(new ItemStack(Items.LAPIS_LAZULI, 4 + RNG.nextInt(8)));
            }

            event.getDrops().addAll(bonusDrops);
            ArcaneForge.LOGGER.debug("EtherealHook L{}: {} bonus drops", level, bonusDrops.size());
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("EtherealHook error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. SOUL DELVE — Pala/Herramienta con drops de XP extra
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onSoulDelve(BlockDropsEvent event) {
        try {
            Entity breaker = event.getBreaker();
            if (!(breaker instanceof Player player)) return;
            if (player.level().isClientSide()) return;

            ItemStack tool = player.getMainHandItem();
            int level = lvl(player.level(), tool, SOUL_DELVE);
            if (level <= 0) return;

            // XP bonus (topado: sin esto, a nivel 255 via la Arcane Forge esto
            // daba miles de puntos de XP por bloque roto). Techo re-escalado de
            // 30 a 100 para que invertir mas en la Forja siga dando mas XP.
            int cappedLevel = Math.min(level, 100);
            int xpBonus = 2 + cappedLevel * 3 + RNG.nextInt(cappedLevel * 2 + 1);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.levelEvent(1001, event.getPos(), 0); // sound
                player.giveExperiencePoints(xpBonus);
            }

            // Duplicar un drop aleatorio con probabilidad basada en nivel
            if (!event.getDrops().isEmpty() && RNG.nextInt(10) < level * 3) {
                int idx = RNG.nextInt(event.getDrops().size());
                ItemEntity src = event.getDrops().get(idx);
                ItemStack dup = src.getItem().copy();
                dup.setCount(Math.min(dup.getCount(), dup.getMaxStackSize()));
                event.getDrops().add(new ItemEntity(
                        player.level(),
                        src.getX(), src.getY(), src.getZ(), dup));
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("SoulDelve error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. ARCANE CATACLYSM — Mazo con explosión en área
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onArcaneCataclysm(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            Entity attacker = event.getSource().getEntity();
            if (!(attacker instanceof LivingEntity livingAttacker)) return;

            ItemStack weapon = livingAttacker.getMainHandItem();
            if (!weapon.is(Items.MACE)) return;

            int level = lvl(livingAttacker.level(), weapon, ARCANE_CATACLYSM);
            if (level <= 0) return;
            if (!(livingAttacker.level() instanceof ServerLevel serverLevel)) return;

            Vec3 pos = target.position();

            // Knockback AOE a todos en radio (radio y empuje topados: la Arcane
            // Forge permite subir esto hasta nivel 255 con Pedestal, asi que el
            // radio de busqueda de entidades y el empuje necesitan un techo para
            // no dispararse). Pendientes mas suaves que antes para que sigan
            // creciendo en vez de aplanarse ya a nivel ~9-14.
            double radius = Math.min(3.0 + level * 0.15, 25.0);
            double push = Math.min(1.5 + level * 0.05, 15.0);
            AABB area = new AABB(pos.x - radius, pos.y - 1, pos.z - radius,
                                 pos.x + radius, pos.y + 3, pos.z + radius);
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                if (nearby == livingAttacker) continue;
                Vec3 dir = nearby.position().subtract(pos).normalize();
                nearby.push(dir.x * push, Math.min(0.5 + level * 0.05, 3.0), dir.z * push);
            }

            // Bonus damage escalable, re-topado en x20 el daño original (era x5,
            // que se alcanzaba ya a nivel 9). Sigue siendo un instakill a niveles
            // muy altos — es intencional, invertir hasta el techo real (250) debe
            // premiar de verdad.
            float bonus = event.getOriginalDamage() * Math.min(0.5f + level * 0.06f, 20.0f);
            event.setNewDamage(event.getNewDamage() + bonus);

            // Pequeña explosión sin daño de bloques
            float expRadius = Math.min(1.5f + level * 0.02f, 8.0f);
            serverLevel.explode(null, pos.x, pos.y, pos.z, expRadius,
                    false, ServerLevel.ExplosionInteraction.NONE);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneCataclysm error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. VOID WINGS — Élitro sin daño de caída + velocidad
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onVoidWingsfall(LivingFallEvent event) {
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            int level = lvl(player.level(), chest, VOID_WINGS);
            if (level <= 0) return;

            // Cancelar daño de caída completamente
            event.setDamageMultiplier(0.0f);
            // Reducir distancia para que no active efectos de sonido
            event.setDistance(0.0f);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("VoidWings fall error: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onVoidWingsTick(PlayerTickEvent.Post event) {
        try {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;

            ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            int level = lvl(player.level(), chest, VOID_WINGS);
            if (level <= 0) return;

            // Si vuela con élitro, dar boost de velocidad y proteger durabilidad
            if (player.isFallFlying()) {
                // Speed effect mientras vuela
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40,
                        Math.min(level - 1, 3), false, false, true));

                // A nivel 3+ proteger el élitro de perder durabilidad
                if (level >= 3 && chest.is(Items.ELYTRA) && chest.getDamageValue() > 0) {
                    chest.setDamageValue(0);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("VoidWings tick error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. CHAIN THUNDER — Tridente con rayos en cadena
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onChainThunder(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            Entity attacker = event.getSource().getEntity();
            if (!(attacker instanceof LivingEntity livingAttacker)) return;

            // Detectar si el daño vino de un tridente (tipo de daño "trident")
            String msgId = event.getSource().type().msgId();
            if (!msgId.contains("trident")) return;

            // En supervivencia, TridentItem.releaseUsing() ya CONSUMIO el
            // tridente de la mano del jugador antes de que el proyectil golpee
            // (itemStack.consumeAndReturn(1, player)), asi que para un golpe
            // LANZADO getMainHandItem() ya esta vacio/con otra cosa y el rayo
            // nunca se activaba — por eso "solo funcionaba en creativo"
            // (hasInfiniteMaterials() evita esa consumicion). El proyectil
            // (ThrownTrident extends AbstractArrow) siempre guarda su propia
            // copia del item lanzado via getPickupItemStackOrigin(), con
            // encantamientos incluidos, sin importar el modo de juego.
            ItemStack weapon = ItemStack.EMPTY;
            Entity directEntity = event.getSource().getDirectEntity();
            if (directEntity instanceof AbstractArrow arrow) {
                weapon = arrow.getPickupItemStackOrigin();
            }
            if (weapon.isEmpty()) {
                // Golpe cuerpo a cuerpo con el tridente en mano (no fue lanzado).
                weapon = livingAttacker.getMainHandItem();
                if (!weapon.is(Items.TRIDENT)) weapon = livingAttacker.getOffhandItem();
            }
            if (weapon.isEmpty() || !weapon.is(Items.TRIDENT)) return;

            int level = lvl(livingAttacker.level(), weapon, CHAIN_THUNDER);
            if (level <= 0) return;
            if (!(livingAttacker.level() instanceof ServerLevel serverLevel)) return;

            // Rayos en cadena: buscar enemigos cercanos al impacto (radio topado:
            // sin esto, a nivel 255 la busqueda de entidades escanea un area
            // gigante en cada golpe). Techos re-escalados (antes se aplanaban en
            // nivel ~3.5/17.5) para que invertir mas siga dando mas rayos y mas
            // alcance.
            int chains = Math.min(level / 4 + 1, 20);
            double radius = Math.min(5.0 + level * 0.5, 60.0);
            AABB area = target.getBoundingBox().inflate(radius);

            List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area);
            int struck = 0;
            for (LivingEntity e : nearby) {
                if (struck >= chains) break;
                if (e == target || e == livingAttacker) continue;
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                bolt.setPos(e.getX(), e.getY(), e.getZ());
                serverLevel.addFreshEntity(bolt);
                struck++;
            }

            // También golpear al objetivo principal con rayo
            LightningBolt mainBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
            mainBolt.setPos(target.getX(), target.getY(), target.getZ());
            serverLevel.addFreshEntity(mainBolt);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ChainThunder error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. ARCANE REPULSE — Escudo: repeler + reflejar daño
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onArcaneRepulse(LivingDamageEvent.Pre event) {
        try {
            LivingEntity defender = event.getEntity();
            if (defender.level().isClientSide()) return;

            // Solo actúa cuando el defensor está bloqueando con escudo
            if (!defender.isBlocking()) return;

            ItemStack shield = defender.getUseItem();
            if (!shield.is(Items.SHIELD)) return;

            int level = lvl(defender.level(), shield, ARCANE_REPULSE);
            if (level <= 0) return;
            if (!(defender.level() instanceof ServerLevel serverLevel)) return;

            Entity attacker = event.getSource().getEntity();
            Vec3 defPos = defender.position();

            // Knockback AOE en área (radio y empuje topados por la misma razon
            // que en Arcane Cataclysm; pendientes suavizadas para no aplanarse
            // ya a nivel ~10-15 como antes).
            double radius = Math.min(3.0 + level * 0.15, 40.0);
            double push = Math.min(2.0 + level * 0.06, 18.0);
            AABB area = new AABB(defPos.x - radius, defPos.y - 1, defPos.z - radius,
                                 defPos.x + radius, defPos.y + 3, defPos.z + radius);
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                if (nearby == defender) continue;
                Vec3 dir = nearby.position().subtract(defPos).normalize();
                nearby.push(dir.x * push, Math.min(0.4 + level * 0.2, 3.0), dir.z * push);
            }

            // Reflejar % del daño bloqueado de vuelta al atacante
            if (attacker instanceof LivingEntity livingAttacker) {
                float reflect = event.getOriginalDamage() * (0.15f * level);
                livingAttacker.hurt(serverLevel.damageSources().thorns(defender),
                        Math.min(reflect, 20.0f));
            }

            // Reducir el daño recibido mientras se bloquea, PERO nunca a cero:
            // con la formula original, desde nivel 4 (facil de superar via la
            // Arcane Forge, que ignora el max_level=3 del JSON) el multiplicador
            // llegaba a 0 y el jugador quedaba invencible mientras bloquea. Se
            // topa en 10% de daño minimo, siempre pasa algo.
            event.setNewDamage(event.getNewDamage() * Math.max(0.10f, 1.0f - level * 0.3f));

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneRepulse error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. ETERNAL FEAST — Comida que nunca acaba + ruleta de efectos
    // ═══════════════════════════════════════════════════════════════════════════

    private static final long EFFECT_COOLDOWN_TICKS = 600L; // 30 segundos entre tiradas de ruleta

    @SubscribeEvent
    public static void onEternalFeastTick(PlayerTickEvent.Post event) {
        try {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            if (!(player instanceof ServerPlayer sp)) return;

            // Nivel MAS ALTO de Eternal Feast entre todo lo que lleve el jugador,
            // y la lista de TODOS los alimentos encantados que tiene encima
            // (cualquiera con el encantamiento, no hace falta comerselos). El
            // nivel mas alto decide que tan segura es la ruleta — ver
            // applyRoulette(); la lista se usa para darle ademas el efecto
            // "de comerselo" propio de cada alimento — ver applyFoodOwnEffects().
            int feastLevel = 0;
            List<ItemStack> feastItems = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item.isEmpty()) continue;
                int l = lvl(player.level(), item, ETERNAL_FEAST);
                if (l <= 0) continue;
                if (l > feastLevel) feastLevel = l;
                feastItems.add(item);
            }

            if (feastLevel <= 0) return;

            // Mantener hambre siempre llena
            net.minecraft.world.food.FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() < 18) {
                foodData.setFoodLevel(20);
                foodData.setSaturation(10.0f);
            }

            // Ruleta de efectos aleatorios cada N ticks
            long time = player.level().getGameTime();
            if (time % EFFECT_COOLDOWN_TICKS != 0) return;

            applyFoodOwnEffects(sp, feastItems);
            applyRoulette(sp, feastLevel);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("EternalFeast error: {}", e.getMessage());
        }
    }

    /**
     * Ademas de la ruleta, cada alimento encantado tambien concede su propio
     * efecto "al comerlo" — el mismo que le daria al jugador si de verdad se
     * lo comiera (ej. Regeneracion + Absorcion de una Manzana Dorada, o los
     * cuatro efectos de una Manzana Encantada/Notch) — sin necesidad de
     * comerselo, solo por llevarlo encima. Se lee directamente del propio
     * componente "Consumable" del item (el mismo dato que usa el juego para
     * decidir que pasa cuando lo comes), asi que funciona automaticamente
     * con CUALQUIER comida que tenga efectos al comer, incluidos los stews
     * sospechosos y comidas de otros mods — no hay nada hardcodeado aqui.
     * Los alimentos sin efecto propio (pan, carne cocida, etc.) simplemente
     * no aportan nada extra, igual que si te los comieras de verdad.
     */
    private static void applyFoodOwnEffects(ServerPlayer player, List<ItemStack> feastItems) {
        Level level = player.level();
        java.util.Set<net.minecraft.world.item.Item> applied = new java.util.HashSet<>();
        for (ItemStack stack : feastItems) {
            if (!applied.add(stack.getItem())) continue; // no repetir el mismo alimento varias veces
            net.minecraft.world.item.component.Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            if (consumable == null) continue;
            for (net.minecraft.world.item.consume_effects.ConsumeEffect effect : consumable.onConsumeEffects()) {
                try {
                    effect.apply(level, stack, player);
                } catch (Exception e) {
                    ArcaneForge.LOGGER.debug("EternalFeast food-effect error: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Nivel maximo real alcanzable en la Forja Arcana con Pedestal activo
     * (ver ArcaneForgeBlockEntity: 15 sin Pedestal, 255 con Pedestal). La
     * ruleta usa este techo como referencia para decidir cuando ya no debe
     * quedar NINGUN efecto malo.
     */
    private static final int MAX_FORGE_LEVEL = 255;

    /**
     * Probabilidad (0-100) de caer en un efecto MALO segun el nivel de
     * Eternal Feast. Cuatro tramos que van reduciendo el riesgo a medida
     * que se invierte mas en subirlo de nivel en la Forja, hasta llegar a
     * CERO exactamente en el nivel maximo (255): a partir de ahi la ruleta
     * es 100% segura, solo efectos buenos.
     */
    private static int badChancePercent(int level) {
        if (level >= MAX_FORGE_LEVEL) return 0;  // 255: ningun efecto malo, nunca
        if (level >= 200) return 2;              // 200-254: casi nunca
        if (level >= 100) return 5;              // 100-199: poco frecuente
        if (level >= 50)  return 10;             // 50-99: menos comun, y ya no el peor de los dos
        return 20;                               // 1-49: riesgo completo (los 2 efectos malos)
    }

    /** El efecto malo mas duro (Debilidad + Hambre) solo puede salir en niveles bajos. */
    private static boolean severeBadAllowed(int level) {
        return level < 50;
    }

    /** 8 efectos buenos siempre disponibles; 2 efectos malos que se van reduciendo con el nivel (ver arriba). */
    private static void applyRoulette(ServerPlayer player, int level) {
        int badChance = badChancePercent(level);
        if (badChance > 0 && RNG.nextInt(100) < badChance) {
            if (severeBadAllowed(level) && RNG.nextBoolean()) {
                // ¡PELIGRO! Debilidad + hambre severa temporal
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 4));
                ArcaneForge.LOGGER.debug("EternalFeast DANGER for {} (level {})", player.getName().getString(), level);
            } else {
                // ¡RULETA RUSA! Náusea + Ceguera
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                ArcaneForge.LOGGER.debug("EternalFeast BAD LUCK for {} (level {})", player.getName().getString(), level);
            }
            return;
        }

        int roll = RNG.nextInt(8);
        switch (roll) {
            case 0 -> // Regeneración potente
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
            case 1 -> // Velocidad extrema
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 3));
            case 2 -> // Fuerza
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 2));
            case 3 -> // Salto extremo
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 600, 4));
            case 4 -> // Visión nocturna
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
            case 5 -> // Resistencia
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 3));
            case 6 -> { // JACKPOT: todos los buenos + absorción masiva
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 3));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 4));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 4));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 4));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 9));
                player.setHealth(player.getMaxHealth());
                ArcaneForge.LOGGER.debug("EternalFeast JACKPOT for {}", player.getName().getString());
            }
            case 7 -> // Invisibilidad (neutro/bueno)
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. CORAZA ARCANA — reduce TODO el daño, sin pasar por el sistema de
    //    Proteccion vanilla (asi tambien protege contra golpes que ignoran
    //    armadura por diseño, como el grito del Warden — sonic_boom esta en
    //    la etiqueta minecraft:bypasses_armor, asi que ni el mejor casco de
    //    netherita ni Proteccion V hacen nada contra el).
    // ═══════════════════════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCorazaArcana(LivingDamageEvent.Pre event) {
        try {
            LivingEntity defender = event.getEntity();
            if (defender.level().isClientSide()) return;

            int level;
            if (defender instanceof AbstractGolem golem && ArcaneGolemUtil.isArcaneGolem(golem)) {
                // El golem no tiene slot de pecho: su nivel es el vinculado con el cetro.
                level = ArcaneGolemUtil.golemEnchantLevel(golem, CORAZA_ARCANA);
            } else {
                ItemStack chest = defender.getItemBySlot(EquipmentSlot.CHEST);
                level = lvl(defender.level(), chest, CORAZA_ARCANA);
            }
            if (level <= 0) return;

            // "De bandera": escala suave hasta el tope real de 250, con techo
            // firme en 60% de reduccion — nunca invencible, pero subir de nivel
            // en la Forja sigue dando algo hasta cerca del final de la escala.
            float reduction = Math.min(0.10f + 0.002f * level, 0.60f);
            event.setNewDamage(event.getNewDamage() * (1.0f - reduction));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("CorazaArcana error: {}", e.getMessage());
        }
    }
}
