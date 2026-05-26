package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║         SISTEMA DE FUSIÓN DE ENCANTAMIENTOS INCOMPATIBLES          ║
 * ║         Versión Final Corregida (Casteo de Breaker a Player)       ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class EnchantmentFusionHandler {

    private static final Random RANDOM = new Random();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    // ════════════════════════════════════════════════════════════════
    // 1. FORTUNA + TOQUE DE SEDA
    // ════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        try {
            // Conseguir la entidad que rompió el bloque
            Entity breaker = event.getBreaker();

            // Verificar si el "breaker" existe y si es realmente un Jugador
            if (!(breaker instanceof Player player)) return;
            if (player.level().isClientSide()) return;

            ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty()) return;

            Map<String, Integer> enchants = getEnchantmentMap(tool);

            int silkTouchLevel  = enchants.getOrDefault("silk_touch", 0);
            int fortuneLevel    = enchants.getOrDefault("fortune", 0);

            // VERIFICAR: Solo activar fusión si hay Pedestal Arcano activo cerca
            boolean hasActivePedestal = false;
            try {
                var bePos = player.blockPosition();
                var level = player.level();
                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            var pos = bePos.offset(x, y, z);
                            var state = level.getBlockState(pos);
                            if (state.getBlock() instanceof ArcanePedestalBlock) {
                                hasActivePedestal = true;
                                break;
                            }
                        }
                        if (hasActivePedestal) break;
                    }
                    if (hasActivePedestal) break;
                }
            } catch (Exception e) {
                ArcaneForge.LOGGER.warn("Error verificando pedestal: {}", e.getMessage());
            }

            // Si no tiene la fusión activa O no hay pedestal, ignoramos
            if (silkTouchLevel <= 0 || fortuneLevel <= 0 || !hasActivePedestal) return;

            BlockState brokenState = event.getState();
            BlockPos blockPos = event.getPos();

            // Calcular el número de drops extras por Fortuna
            int extraDrops = RANDOM.nextInt(fortuneLevel + 1);

            if (extraDrops > 0 && player.level() instanceof ServerLevel serverLevel) {
                ItemStack silkDrop = new ItemStack(brokenState.getBlock().asItem());
                if (!silkDrop.isEmpty()) {
                    for (int i = 0; i < extraDrops; i++) {
                        ItemEntity itemEntity = new ItemEntity(
                                serverLevel,
                                blockPos.getX() + 0.5,
                                blockPos.getY() + 0.5,
                                blockPos.getZ() + 0.5,
                                silkDrop.copy()
                        );
                        itemEntity.setDefaultPickUpDelay();

                        // Añadir los elementos extras directamente a la lista de drops del evento
                        event.getDrops().add(itemEntity);
                    }
                    
                    String toolType = tool.getItem().toString().contains("axe") ? "Hacha" : 
                                     tool.getItem().toString().contains("pickaxe") ? "Pico" : "Herramienta";
                    
                    ArcaneForge.LOGGER.debug(
                            "Fusion Core: Fortune {} + Silk Touch en {} con Pedestal -> Añadidas {} copias extra de {}",
                            fortuneLevel, toolType, extraDrops, silkDrop.getHoverName().getString());
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Fusion block drops handler error: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. DAÑO COMBINADO + 3. PROTECCIÓN MÚLTIPLE
    // ════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            DamageSource source = event.getSource();
            float originalDamage = event.getOriginalDamage();

            // Protección múltiple
            float protectionReduction = calculateFusedProtection(target, source);
            if (protectionReduction > 0) {
                float newDamage = Math.max(0, originalDamage * (1.0f - protectionReduction));
                event.setNewDamage(newDamage);
                ArcaneForge.LOGGER.debug("Fusion Protection: {} -> {} ({}% reduction)",
                        originalDamage, newDamage, (int)(protectionReduction * 100));
            }

            // Daño combinado del atacante
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                float extraDamage = calculateFusedDamage(livingAttacker, target);
                if (extraDamage > 0) {
                    float current = event.getNewDamage();
                    event.setNewDamage(current + extraDamage);
                    ArcaneForge.LOGGER.debug("Fusion Damage: {} + {} extra = {}",
                            current, extraDamage, current + extraDamage);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Fusion damage handler error: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // PROTECCIÓN FUSIONADA
    // ════════════════════════════════════════════════════════════════

    private static float calculateFusedProtection(LivingEntity target, DamageSource source) {
        float totalReduction = 0;
        int protectionTypes = 0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getItemBySlot(slot);
            if (armor.isEmpty()) continue;

            Map<String, Integer> enchants = getEnchantmentMap(armor);

            int protLevel = enchants.getOrDefault("protection", 0);
            if (protLevel > 0) {
                totalReduction += protLevel * 0.04f;
                protectionTypes++;
            }

            int fireProtLevel = enchants.getOrDefault("fire_protection", 0);
            if (fireProtLevel > 0 && isDamageType(source,
                    "fire", "in_fire", "on_fire", "lava", "hot_floor")) {
                totalReduction += fireProtLevel * 0.08f;
                protectionTypes++;
            }

            int blastProtLevel = enchants.getOrDefault("blast_protection", 0);
            if (blastProtLevel > 0 && isDamageType(source,
                    "explosion", "player_explosion")) {
                totalReduction += blastProtLevel * 0.08f;
                protectionTypes++;
            }

            int projProtLevel = enchants.getOrDefault("projectile_protection", 0);
            if (projProtLevel > 0 && isDamageType(source,
                    "arrow", "trident", "mob_projectile")) {
                totalReduction += projProtLevel * 0.08f;
                protectionTypes++;
            }
        }

        if (protectionTypes <= 1) return 0;
        return Math.min(totalReduction, 0.80f);
    }

    // ════════════════════════════════════════════════════════════════
    // DAÑO FUSIONADO
    // ════════════════════════════════════════════════════════════════

    private static float calculateFusedDamage(LivingEntity attacker, LivingEntity target) {
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return 0;

        Map<String, Integer> enchants = getEnchantmentMap(weapon);

        int sharpnessLevel = enchants.getOrDefault("sharpness", 0);
        int smiteLevel     = enchants.getOrDefault("smite", 0);
        int baneLevel      = enchants.getOrDefault("bane_of_arthropods", 0);

        int damageEnchantCount = 0;
        if (sharpnessLevel > 0) damageEnchantCount++;
        if (smiteLevel > 0)     damageEnchantCount++;
        if (baneLevel > 0)      damageEnchantCount++;
        if (damageEnchantCount <= 1) return 0;

        float extraDamage = 0;
        boolean isUndead     = isUndeadMob(target);
        boolean isArthropod  = isArthropodMob(target);

        if (sharpnessLevel > 0 && (smiteLevel > 0 || baneLevel > 0)) {
            extraDamage += 0.5f + (0.5f * sharpnessLevel);
        }
        if (smiteLevel > 0 && isUndead && sharpnessLevel > 0) {
            extraDamage += 2.5f * smiteLevel;
        }
        if (baneLevel > 0 && isArthropod && sharpnessLevel > 0) {
            extraDamage += 2.5f * baneLevel;
        }

        return extraDamage;
    }

    // ════════════════════════════════════════════════════════════════
    // DETECCIÓN DE TIPO DE MOB
    // ════════════════════════════════════════════════════════════════

    private static boolean isUndeadMob(LivingEntity entity) {
        try {
            String typePath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

            return switch (typePath) {
                case "zombie", "husk", "drowned", "zombie_villager",
                     "zombie_horse", "skeleton", "skeleton_horse",
                     "wither_skeleton", "stray", "phantom",
                     "wither", "zoglin", "zombified_piglin",
                     "giant", "drowned_zombie" -> true;
                default ->
                        typePath.contains("zombie") || typePath.contains("skeleton")
                                || typePath.contains("undead") || typePath.contains("wither")
                                || typePath.contains("phantom") || typePath.contains("revenant");
            };
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isArthropodMob(LivingEntity entity) {
        try {
            String typePath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

            return switch (typePath) {
                case "spider", "cave_spider", "silverfish",
                     "endermite", "bee" -> true;
                default ->
                        typePath.contains("spider") || typePath.contains("silverfish")
                                || typePath.contains("endermite") || typePath.contains("bee")
                                || typePath.contains("arthropod") || typePath.contains("insect");
            };
        } catch (Exception e) {
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════════

    private static Map<String, Integer> getEnchantmentMap(ItemStack stack) {
        Map<String, Integer> result = new HashMap<>();
        ItemEnchantments enchants = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            try {
                var keyOpt = holder.unwrapKey();
                if (keyOpt.isPresent()) {
                    String path = keyOpt.get().identifier().getPath();
                    result.put(path, level);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static boolean isDamageType(DamageSource source, String... typeNames) {
        try {
            String sourceType = source.type().msgId();
            for (String name : typeNames) {
                if (sourceType.contains(name)) return true;
            }
            var keyOpt = source.typeHolder().unwrapKey();
            if (keyOpt.isPresent()) {
                String path = keyOpt.get().identifier().getPath();
                for (String name : typeNames) {
                    if (path.contains(name)) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}