package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La causa real de que el Cofre Infinito siguiera limitado a 64/99: TODO el
 * codigo de clicks/drag/shift-click (AbstractContainerMenu.moveItemStackTo,
 * doClick, quickMoveStack...) consulta Slot.getMaxStackSize(ItemStack), NO
 * container.getMaxStackSize(ItemStack) directamente. Y Slot ya tiene su
 * PROPIA implementacion concreta:
 *
 *   public int getMaxStackSize(ItemStack itemStack) {
 *       return Math.min(this.getMaxStackSize(), itemStack.getMaxStackSize());
 *   }
 *
 * getMaxStackSize() (sin args) SI delega al contenedor (y ahi nuestro
 * InfiniteChestCapacityMixin/CompoundContainerCapacityMixin ya devolvian
 * 1_000_000_000 correctamente) — pero esa linea vuelve a recortar el
 * resultado con itemStack.getMaxStackSize(), el limite propio del ITEM
 * (64 para tierra, diamantes, etc.). Por eso nuestros mixins en el
 * contenedor nunca importaban: Slot los ignoraba con este segundo
 * Math.min. Interceptamos aqui, en el unico punto que de verdad usa toda
 * la logica de clicks del juego.
 */
@Mixin(Slot.class)
public abstract class InfiniteChestSlotMixin {

    private static final int INFINITE_MAX_STACK = 1_000_000_000;

    @Shadow
    @Final
    public Container container;

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$bypassItemCap(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (this.container instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite()) {
            cir.setReturnValue(INFINITE_MAX_STACK);
        }
    }
}
