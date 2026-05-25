package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class AxeFortuneHandler {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Entity breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !(tool.getItem() instanceof AxeItem)) return;

        BlockState state = event.getState();
        // Filtro nativo NeoForge para detectar cualquier tronco u hojas de árbol
        if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.SAPLINGS)) return;

        int fortuneLevel = 0;
        var enchantmentRegistry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var fortuneHolder = enchantmentRegistry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.withDefaultNamespace("fortune")));
        if (fortuneHolder.isPresent()) {
            fortuneLevel = tool.getEnchantmentLevel(fortuneHolder.get());
        }

        if (fortuneLevel > 0) {
            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack stack = itemEntity.getItem();
                if (stack.isStackable()) {
                    int extra = player.level().getRandom().nextInt(fortuneLevel + 1);
                    if (extra > 0) {
                        ItemStack newStack = stack.copy();
                        newStack.setCount(Math.min(newStack.getMaxStackSize(), newStack.getCount() + extra));
                        itemEntity.setItem(newStack);
                    }
                }
            }
        }
    }
}