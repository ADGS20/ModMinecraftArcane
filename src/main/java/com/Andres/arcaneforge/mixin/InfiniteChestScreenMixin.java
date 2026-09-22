package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * El Cofre Infinito guarda cantidades enormes por slot (ver
 * InfiniteChestCapacityMixin), pero el juego dibuja el numero crudo tal cual
 * (ej. "23025") sin abreviar, lo que no cabe en el icono de 16x16 y se ve
 * roto — a diferencia del Saco Dimensional, que ya abrevia con formatCompact
 * (ver DimensionalBagScreen). Como la pantalla de cofre vainilla no es
 * nuestra clase, no podemos simplemente sobreescribir renderSlotContents;
 * interceptamos la llamada a GuiGraphicsExtractor.itemDecorations(...) (el
 * metodo que dibuja el numero) tanto en el slot normal como en el item
 * "agarrado" con el mouse, y solo cuando el contenedor detras del menu es un
 * Cofre Infinito reemplazamos el texto por el mismo formato compacto.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class InfiniteChestScreenMixin {

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    private boolean arcaneforge$isInfiniteChestMenu() {
        if (!(this.menu instanceof ChestMenu chestMenu)) return false;
        Container container = chestMenu.getContainer();
        return container instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite();
    }

    private static String arcaneforge$formatCompact(int count) {
        if (count < 1_000) return String.valueOf(count);
        if (count < 1_000_000) return (count / 1_000) + "K";
        if (count < 1_000_000_000) return (count / 1_000_000) + "M";
        return (count / 1_000_000_000) + "B";
    }

    @ModifyArg(
            method = "renderSlotContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            index = 4
    )
    private String arcaneforge$formatSlotCount(Font font, ItemStack itemStack, int x, int y, String itemCount) {
        if (itemCount == null && itemStack.getCount() >= 1_000 && arcaneforge$isInfiniteChestMenu()) {
            return arcaneforge$formatCompact(itemStack.getCount());
        }
        return itemCount;
    }

    @ModifyArg(
            method = "extractFloatingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            index = 4
    )
    private String arcaneforge$formatCarriedCount(Font font, ItemStack itemStack, int x, int y, String itemCount) {
        if (itemCount == null && itemStack.getCount() >= 1_000 && arcaneforge$isInfiniteChestMenu()) {
            return arcaneforge$formatCompact(itemStack.getCount());
        }
        return itemCount;
    }
}
