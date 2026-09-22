package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Cuando colocas un cofre (item) que tiene el encantamiento almacen_infinito,
 * marcamos el BlockEntity recien colocado como "infinito" via
 * IArcaneInfiniteChest — a partir de ahi InfiniteChestCapacityMixin le sube
 * el limite de stack por slot e InfiniteChestBlockEntityMixin se encarga de
 * guardarlo/cargarlo sin perder la cantidad real.
 *
 * IMPORTANTE: BlockItem.place() consume 1 del ItemStack (itemStack.consume(1,
 * player)) justo antes de retornar — si el jugador coloca la ULTIMA copia,
 * el stack queda vacio (ItemStack.EMPTY) y pierde sus DataComponents,
 * incluidos los encantamientos. Por eso el encantamiento se lee en HEAD
 * (stack intacto) y se aplica recien en RETURN (cuando el BlockEntity ya
 * existe) — igual que tuvimos que hacer con el tridente en supervivencia.
 */
@Mixin(BlockItem.class)
public abstract class InfiniteChestPlaceMixin {

    private static final ResourceKey<Enchantment> ALMACEN_INFINITO_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "almacen_infinito"));

    @Unique
    private boolean arcaneforge$pendingInfinite = false;

    @Inject(method = "place", at = @At("HEAD"))
    private void arcaneforge$readEnchantBeforeConsume(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        this.arcaneforge$pendingInfinite = false;
        try {
            Level level = placeContext.getLevel();
            if (level.isClientSide()) return;

            ItemStack stack = placeContext.getItemInHand();
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(ALMACEN_INFINITO_KEY);
            if (opt.isEmpty()) return;

            ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            int lvl = enchants.getLevel(opt.get());
            this.arcaneforge$pendingInfinite = lvl > 0;
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("[INFCHEST-PLACE] read error: {}", e.getMessage());
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void arcaneforge$markInfiniteChest(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        boolean shouldMark = this.arcaneforge$pendingInfinite;
        this.arcaneforge$pendingInfinite = false;
        if (!shouldMark) return;
        try {
            Level level = placeContext.getLevel();
            if (level.isClientSide()) return;

            BlockEntity be = level.getBlockEntity(placeContext.getClickedPos());
            if (be instanceof IArcaneInfiniteChest chest) {
                chest.arcaneforge$setInfinite(true);
                be.setChanged();
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("[INFCHEST-PLACE] mark error: {}", e.getMessage());
        }
    }
}
