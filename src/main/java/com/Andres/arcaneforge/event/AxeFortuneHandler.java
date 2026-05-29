package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;

// CORRECCIÓN: En NeoForge 1.21 ya no se usa el parámetro 'bus', se auto-detecta.
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class AxeFortuneHandler {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Entity breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) return;

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) return;

        // 1. RESTRICCIÓN DE MOD: ¿Tiene el sello "ArcaneAwakened" otorgado por el Pedestal?
        CustomData customData = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains("ArcaneAwakened")) {
            return; // Hacha normal o encantada en Yunque. Ignoramos y dejamos la regla vanilla.
        }

        // 2. Si tiene la marca y estamos talando madera
        if (event.getState().is(BlockTags.LOGS)) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            try {
                var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                Optional<Holder.Reference<Enchantment>> fortuneOpt = registry.get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);

                if (fortuneOpt.isPresent()) {
                    int fortuneLevel = enchantments.getLevel(fortuneOpt.get());

                    if (fortuneLevel > 0) {
                        for (ItemEntity drop : event.getDrops()) {
                            ItemStack dropStack = drop.getItem();

                            // 3. Aplicamos el efecto de poder: Añadimos un tronco extra por cada nivel de fortuna
                            dropStack.setCount(dropStack.getCount() + fortuneLevel);
                            drop.setItem(dropStack);
                        }
                    }
                }
            } catch (Exception e) {
                ArcaneForge.LOGGER.error("Error en multiplicador Arcane Fortune: " + e.getMessage());
            }
        }
    }
}