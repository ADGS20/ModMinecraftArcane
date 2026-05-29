package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneBowFusionHandler {

    @SubscribeEvent
    public static void onBowDamage(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            DamageSource source = event.getSource();
            Entity attacker = source.getEntity();

            // Comprobamos si el atacante existe y si el daño provino de un proyectil a distancia
            if (attacker instanceof LivingEntity livingAttacker && source.getDirectEntity() != null && !source.getDirectEntity().equals(attacker)) {
                ItemStack bow = livingAttacker.getUseItem();
                if (bow.isEmpty()) bow = livingAttacker.getMainHandItem();
                if (bow.isEmpty()) bow = livingAttacker.getOffhandItem();

                if (!bow.isEmpty()) {
                    CustomData customData = bow.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

                    // Verificamos si el arco tiene la marca arcana del pedestal
                    if (customData.contains("ArcaneAwakened")) {
                        ItemEnchantments enchants = bow.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                        var registry = livingAttacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                        var mendingOpt = registry.get(net.minecraft.world.item.enchantment.Enchantments.MENDING);
                        var infinityOpt = registry.get(net.minecraft.world.item.enchantment.Enchantments.INFINITY);

                        if (mendingOpt.isPresent() && infinityOpt.isPresent()) {
                            int mendingLvl = enchants.getLevel(mendingOpt.get());
                            int infinityLvl = enchants.getLevel(infinityOpt.get());

                            // Si se detecta la fusión legendaria de Infinidad + Reparación
                            if (mendingLvl > 0 && infinityLvl > 0) {
                                float currentDmg = event.getNewDamage();
                                // Calculamos el multiplicador prohibido
                                float bowExtraDamage = (float) ((mendingLvl * 1.5f) + (infinityLvl * 0.5f));
                                event.setNewDamage(currentDmg + bowExtraDamage);

                                ArcaneForge.LOGGER.debug("Arcane Bow Fusion: +{} damage applied safely!", bowExtraDamage);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Arcane Bow Fusion handler error: {}", e.getMessage());
        }
    }
}