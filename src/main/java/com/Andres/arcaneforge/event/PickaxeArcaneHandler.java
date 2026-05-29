package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class PickaxeArcaneHandler {

    private static final ResourceKey<Enchantment> ARCANE_SMELT_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_smelting"));

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        // CORRECCIÓN 1: Validamos que el breaker exista y sea un Player antes de pedir la mano
        if (event.getBreaker() == null || event.getLevel().isClientSide() || !(event.getBreaker() instanceof Player player)) return;

        ItemStack tool = player.getMainHandItem();
        ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> smeltOpt = registry.get(ARCANE_SMELT_KEY);

            if (smeltOpt.isPresent() && enchants.getLevel(smeltOpt.get()) > 0) {
                var fortuneOpt = registry.get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
                int fortuneLevel = fortuneOpt.isPresent() ? enchants.getLevel(fortuneOpt.get()) : 0;

                event.getDrops().forEach(itemEntity -> {
                    ItemStack dropStack = itemEntity.getItem();
                    ItemStack fusedResult = getSmeltingResult(dropStack, event.getLevel().getRandom(), fortuneLevel);

                    if (!fusedResult.isEmpty()) {
                        itemEntity.setItem(fusedResult);
                    }
                });
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en el Handler de Fundición Arcana: {}", e.getMessage());
        }
    }

    private static ItemStack getSmeltingResult(ItemStack originalDrop, net.minecraft.util.RandomSource random, int fortuneLevel) {
        ItemStack result = ItemStack.EMPTY;

        if (originalDrop.is(Items.RAW_IRON) || originalDrop.is(Items.IRON_ORE) || originalDrop.is(Items.DEEPSLATE_IRON_ORE)) {
            result = new ItemStack(Items.IRON_INGOT);
        }
        // CORRECCIÓN 2: Cambiado DEEPSLATE_GOLD_GOLD_ORE por DEEPSLATE_GOLD_ORE
        else if (originalDrop.is(Items.RAW_GOLD) || originalDrop.is(Items.GOLD_ORE) || originalDrop.is(Items.DEEPSLATE_GOLD_ORE) || originalDrop.is(Items.NETHER_GOLD_ORE)) {
            result = new ItemStack(Items.GOLD_INGOT);
        } else if (originalDrop.is(Items.RAW_COPPER) || originalDrop.is(Items.COPPER_ORE) || originalDrop.is(Items.DEEPSLATE_COPPER_ORE)) {
            result = new ItemStack(Items.COPPER_INGOT);
        }

        if (!result.isEmpty()) {
            int count = originalDrop.getCount();
            if (fortuneLevel > 0) {
                count *= (1 + random.nextInt(fortuneLevel + 1));
            }
            result.setCount(count);
        }

        return result;
    }
}