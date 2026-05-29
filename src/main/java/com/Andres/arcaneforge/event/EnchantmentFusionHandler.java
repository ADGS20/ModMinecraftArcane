package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class EnchantmentFusionHandler {

    private static final Random RANDOM = new Random();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        try {
            Entity breaker = event.getBreaker();

            if (!(breaker instanceof Player player)) return;
            if (player.level().isClientSide()) return;

            ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty()) return;

            Map<String, Integer> enchants = getEnchantmentMap(tool);

            int silkTouchLevel  = enchants.getOrDefault("silk_touch", 0);
            int fortuneLevel    = enchants.getOrDefault("fortune", 0);

            if (silkTouchLevel <= 0 || fortuneLevel <= 0) return;

            BlockPos blockPos = event.getPos();

            // Verificar la presencia del bloque de poder o el pedestal cerca
            boolean hasRequiredBlock = false;
            int radius = 5;
            for (BlockPos p : BlockPos.betweenClosed(blockPos.offset(-radius, -radius, -radius), blockPos.offset(radius, radius, radius))) {
                BlockState checkState = player.level().getBlockState(p);
                if (checkState.is(ModBlocks.ARCANE_POWER_BLOCK.get()) || checkState.is(ModBlocks.ARCANE_PEDESTAL.get())) {
                    hasRequiredBlock = true;
                    break;
                }
            }

            if (!hasRequiredBlock) return;

            int extraDrops = RANDOM.nextInt(fortuneLevel + 1);

            // CORRECCIÓN CLAVE: Generar nuevas entidades independientes para evitar errores de sincronización de Mojang
            if (extraDrops > 0 && !event.getDrops().isEmpty()) {
                List<ItemEntity> extraDropsEntities = new ArrayList<>();

                for (ItemEntity itemEntity : event.getDrops()) {
                    ItemStack stack = itemEntity.getItem();
                    if (!stack.isEmpty()) {
                        int remainingExtra = extraDrops;

                        // Divide de forma segura cantidades masivas en stacks de 64 sin topar el juego
                        while (remainingExtra > 0) {
                            int splitCount = Math.min(stack.getMaxStackSize(), remainingExtra);
                            ItemStack extraStack = stack.copy();
                            extraStack.setCount(splitCount);

                            ItemEntity newEntity = new ItemEntity(
                                    player.level(),
                                    itemEntity.getX(),
                                    itemEntity.getY(),
                                    itemEntity.getZ(),
                                    extraStack
                            );
                            newEntity.setDefaultPickUpDelay();
                            extraDropsEntities.add(newEntity);

                            remainingExtra -= splitCount;
                        }
                    }
                }
                // Añadir de forma segura los nuevos objetos a la lista de recolección de drops de NeoForge
                event.getDrops().addAll(extraDropsEntities);
                ArcaneForge.LOGGER.debug("Fusion Core: Multiplicados drops por Toque de Seda + Fortuna {} con Pedestal/Bloque", fortuneLevel);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Fusion block drops handler error: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            DamageSource source = event.getSource();
            float originalDamage = event.getOriginalDamage();

            float protectionReduction = calculateFusedProtection(target, source);
            if (protectionReduction > 0) {
                float newDamage = Math.max(0, originalDamage * (1.0f - protectionReduction));
                event.setNewDamage(newDamage);
            }

            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                float extraDamage = calculateFusedDamage(livingAttacker, target);
                if (extraDamage > 0) {
                    float current = event.getNewDamage();
                    event.setNewDamage(current + extraDamage);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Fusion damage handler error: {}", e.getMessage());
        }
    }

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
            if (fireProtLevel > 0 && isDamageType(source, "fire", "in_fire", "on_fire", "lava", "hot_floor")) {
                totalReduction += fireProtLevel * 0.08f;
                protectionTypes++;
            }

            int blastProtLevel = enchants.getOrDefault("blast_protection", 0);
            if (blastProtLevel > 0 && isDamageType(source, "explosion", "player_explosion")) {
                totalReduction += blastProtLevel * 0.08f;
                protectionTypes++;
            }

            int projProtLevel = enchants.getOrDefault("projectile_protection", 0);
            if (projProtLevel > 0 && isDamageType(source, "arrow", "trident", "mob_projectile")) {
                totalReduction += projProtLevel * 0.08f;
                protectionTypes++;
            }
        }

        if (protectionTypes <= 1) return 0;
        return Math.min(totalReduction, 0.80f);
    }

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

    private static boolean isUndeadMob(LivingEntity entity) {
        try {
            String typePath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            return switch (typePath) {
                case "zombie", "husk", "drowned", "zombie_villager", "skeleton", "wither_skeleton", "phantom", "wither" -> true;
                default -> typePath.contains("zombie") || typePath.contains("skeleton") || typePath.contains("undead");
            };
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isArthropodMob(LivingEntity entity) {
        try {
            String typePath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            return switch (typePath) {
                case "spider", "cave_spider", "silverfish", "endermite", "bee" -> true;
                default -> typePath.contains("spider") || typePath.contains("arthropod");
            };
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, Integer> getEnchantmentMap(ItemStack stack) {
        Map<String, Integer> result = new HashMap<>();
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

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
        } catch (Exception ignored) {}
        return false;
    }
}