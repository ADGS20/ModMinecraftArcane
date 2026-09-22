package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un "Cofre grande" (dos cofres juntos) no abre un ChestBlockEntity — el
 * juego los envuelve en un CompoundContainer, una clase totalmente aparte
 * que NO hereda InfiniteChestCapacityMixin. Su getMaxStackSize() delega solo
 * a container1 (asi que si el lado infinito quedaba como container2, ni
 * siquiera el limite general se enteraba), y su getMaxStackSize(ItemStack)
 * ni siquiera esta sobreescrito: usa el default de la interfaz Container,
 * que vuelve a limitar con Math.min(this.getMaxStackSize(),
 * itemStack.getMaxStackSize()) — con comida (max 64 en vainilla) el
 * resultado siempre es 64, sin importar que uno de los dos cofres sea
 * infinito. Al implementar aqui la misma interfaz IArcaneInfiniteChest,
 * tanto InfiniteChestScreenMixin (para el numero compacto en pantalla) como
 * cualquier otro chequeo "instanceof IArcaneInfiniteChest" funcionan igual
 * para cofres grandes: basta con que UNO de los dos lados este encantado
 * para que el cofre grande entero se trate como infinito.
 */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerCapacityMixin implements IArcaneInfiniteChest {

    private static final int INFINITE_MAX_STACK = 1_000_000_000;

    @Shadow
    @Final
    private Container container1;

    @Shadow
    @Final
    private Container container2;

    @Override
    public boolean arcaneforge$isInfinite() {
        return (this.container1 instanceof IArcaneInfiniteChest chest1 && chest1.arcaneforge$isInfinite())
                || (this.container2 instanceof IArcaneInfiniteChest chest2 && chest2.arcaneforge$isInfinite());
    }

    @Override
    public void arcaneforge$setInfinite(boolean value) {
        // No-op: un CompoundContainer no se marca directamente, su estado
        // depende de si alguno de los dos cofres que envuelve es infinito.
    }

    /**
     * getMaxStackSize() (sin args) YA es un metodo concreto real en
     * CompoundContainer (delega a container1.getMaxStackSize()), asi que no
     * se puede sobreescribir con un metodo "plano" en el mixin (chocaria).
     * Se intercepta con @Inject cancelable en su lugar.
     */
    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$overrideMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (arcaneforge$isInfinite()) {
            cir.setReturnValue(INFINITE_MAX_STACK);
        }
    }

    /**
     * getMaxStackSize(ItemStack) NO esta sobreescrito en CompoundContainer:
     * hereda el default de la interfaz Container. Aqui SI se puede declarar
     * un metodo concreto normal (sin @Shadow/@Inject) — Mixin lo fusiona
     * como un override real, igual que en InfiniteChestCapacityMixin.
     */
    public int getMaxStackSize(ItemStack itemStack) {
        if (arcaneforge$isInfinite()) return INFINITE_MAX_STACK;
        return Math.min(this.container1.getMaxStackSize(), itemStack.getMaxStackSize());
    }
}
