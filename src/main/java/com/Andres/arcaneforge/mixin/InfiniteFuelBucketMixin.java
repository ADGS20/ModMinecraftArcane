package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * CUBETA INFINITA aplicada al horno: si la cubeta de lava usada como
 * combustible tiene el encantamiento, se salta la llamada a consumeFuel()
 * (que la vaciaria y la cambiaria por una cubeta vacia) y la deja tal cual
 * en la ranura de combustible, lista para volver a arder indefinidamente.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class InfiniteFuelBucketMixin {

    @Unique
    private static final ResourceKey<Enchantment> AF_INFINITE_BUCKET_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "infinite_bucket"));

    @Shadow
    private static void consumeFuel(NonNullList<ItemStack> items, ItemStack fuel) {
        throw new UnsupportedOperationException();
    }

    @Redirect(
            method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;consumeFuel(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;)V"
            )
    )
    private static void arcaneforge$skipFuelConsumptionIfInfinite(
            NonNullList<ItemStack> items, ItemStack fuel,
            ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity) {
        if (isInfiniteBucket(fuel, level)) return;
        consumeFuel(items, fuel);
    }

    @Unique
    private static boolean isInfiniteBucket(ItemStack fuel, ServerLevel level) {
        if (fuel.isEmpty()) return false;
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(AF_INFINITE_BUCKET_KEY);
            if (opt.isEmpty()) return false;

            ItemEnchantments enchants = fuel.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get()) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
