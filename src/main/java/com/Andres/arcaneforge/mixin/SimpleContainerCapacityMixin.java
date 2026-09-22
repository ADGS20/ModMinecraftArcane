package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * La causa de que el CLIENTE siguiera mostrando "64" aunque el servidor ya
 * guardaba mas: el cliente NUNCA usa el ChestBlockEntity real para dibujar
 * la pantalla del cofre. Cuando el servidor abre un menu de cofre, el
 * cliente solo recibe un ClientboundOpenScreenPacket (que menu type
 * mostrar) y construye su PROPIO ChestMenu con un SimpleContainer nuevo y
 * vacio (ChestMenu.threeRows/sixRows(id, inventory) — sin el
 * ChestBlockEntity real, el cliente no tiene forma de saber la posicion).
 * El contenido llega despues por paquetes de sync, y SimpleContainer.setItem
 * hace SIEMPRE itemStack.limitSize(this.getMaxStackSize(itemStack)) — y
 * como SimpleContainer no sabe que este cofre en particular es infinito,
 * recorta cada stack recibido a 64/99 EN EL CLIENTE (el servidor sigue
 * teniendo el numero real intacto). Por eso el conteo interno era correcto
 * pero la pantalla mostraba 64.
 *
 * Este mixin permite que un SimpleContainer TAMBIEN pueda marcarse como
 * infinito (igual que ChestBlockEntity y CompoundContainer). La marca se la
 * pone InfiniteChestSyncHandler cuando el servidor detecta, al abrir el
 * menu, que el cofre real es infinito (ver PlayerContainerEvent.Open) y
 * envia un paquete S2CInfiniteChestSync al cliente para que marque el
 * SimpleContainer del menu recien abierto.
 */
@Mixin(SimpleContainer.class)
public abstract class SimpleContainerCapacityMixin implements IArcaneInfiniteChest {

    private static final int INFINITE_MAX_STACK = 1_000_000_000;

    @Unique
    private boolean arcaneforge$infinite = false;

    @Override
    public void arcaneforge$setInfinite(boolean value) {
        this.arcaneforge$infinite = value;
    }

    @Override
    public boolean arcaneforge$isInfinite() {
        return this.arcaneforge$infinite;
    }

    public int getMaxStackSize() {
        if (this.arcaneforge$infinite) return INFINITE_MAX_STACK;
        return 99;
    }

    public int getMaxStackSize(ItemStack itemStack) {
        if (this.arcaneforge$infinite) return INFINITE_MAX_STACK;
        return Math.min(99, itemStack.getMaxStackSize());
    }
}
