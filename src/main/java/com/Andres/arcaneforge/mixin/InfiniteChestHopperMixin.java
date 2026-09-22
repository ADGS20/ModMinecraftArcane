package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Las tolvas NUNCA pasan por Slot ni por Container.getMaxStackSize(ItemStack)
 * (los dos puntos que ya arreglamos para clics manuales, ver
 * InfiniteChestSlotMixin / InfiniteChestCapacityMixin). HopperBlockEntity usa
 * su propia logica interna con TRES puntos ciegos, todos en el mismo
 * limite vainilla del ITEM (64/99), nunca del contenedor:
 *
 * 1) tryMoveInItem() llama primero a canMergeItems(current, itemStack), que
 *    internamente hace "current.getCount() <= current.getMaxStackSize()".
 *    En cuanto el slot destino supera el limite vainilla del item, esta
 *    condicion da false y el merge se descarta ENTERO — nunca se llega
 *    siquiera al calculo de espacio de abajo. Este es el bloqueo real: sin
 *    arreglarlo, la tolva se detiene en seco justo al pasar de 64/99.
 *
 * 2) tryMoveInItem() calcula el espacio libre como
 *    "itemStack.getMaxStackSize() - current.getCount()" — de nuevo el
 *    maximo vainilla del propio item, no el del contenedor.
 *
 * 3) isFullContainer(): antes de intentar empujar items hacia un contenedor
 *    (p.ej. una tolva empujando HACIA nuestro cofre infinito), comprueba
 *    "itemStack.getCount() < itemStack.getMaxStackSize()" para cada slot —
 *    en cuanto un slot supera el limite vainilla, lo marca como "lleno" y
 *    la tolva deja de intentar insertar en el, aunque el cofre aceptara mas.
 *
 * Los tres necesitan @Redirect separados (firmas de metodo distintas).
 * Cuando el contenedor implicado es un Cofre Infinito activo, se usa un
 * maximo artificialmente alto en vez del vainilla.
 */
@Mixin(HopperBlockEntity.class)
public abstract class InfiniteChestHopperMixin {

    private static final int INFINITE_MAX_STACK = 1_000_000_000;

    @Redirect(
            method = "tryMoveInItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;canMergeItems(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private static boolean arcaneforge$canMergeItems(ItemStack a, ItemStack b, Container from, Container container, ItemStack movedStack, int slot, Direction direction) {
        int maxStack = (container instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite())
                ? INFINITE_MAX_STACK
                : a.getMaxStackSize();
        return a.getCount() <= maxStack && ItemStack.isSameItemSameComponents(a, b);
    }

    @Redirect(
            method = "tryMoveInItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int arcaneforge$mergeMaxStack(ItemStack itemStack, Container from, Container container, ItemStack movedStack, int slot, Direction direction) {
        if (container instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite()) {
            return INFINITE_MAX_STACK;
        }
        return itemStack.getMaxStackSize();
    }

    @Redirect(
            method = "isFullContainer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int arcaneforge$fullCheckMaxStack(ItemStack itemStack, Container container, Direction direction) {
        if (container instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite()) {
            return INFINITE_MAX_STACK;
        }
        return itemStack.getMaxStackSize();
    }
}
