package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Container.getMaxStackSize()/getMaxStackSize(ItemStack) son metodos default
 * de la interfaz Container (limite vainilla: 99), y BaseContainerBlockEntity
 * (superclase de ChestBlockEntity, tambien de furnace/hopper/etc.) nunca los
 * sobreescribe — simplemente hereda el default de la interfaz. Declarar aqui
 * una implementacion propia (sin @Shadow/@Inject: un metodo concreto en un
 * mixin sencillamente se fusiona como override real de la clase objetivo)
 * sube el limite SOLO cuando el bloque es un Cofre Infinito (marcado via
 * IArcaneInfiniteChest, ver InfiniteChestBlockEntityMixin). Para cualquier
 * otro contenedor (cofre normal, horno, tolva...) el resultado es identico
 * al de vainilla. setItem() en esta misma clase llama a
 * getMaxStackSize(itemStack) de forma polimorfica, asi que no hace falta
 * tocar nada mas.
 */
@Mixin(BaseContainerBlockEntity.class)
public abstract class InfiniteChestCapacityMixin {

    private static final int INFINITE_MAX_STACK = 1_000_000_000;

    public int getMaxStackSize() {
        if (this instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite()) {
            return INFINITE_MAX_STACK;
        }
        return 99;
    }

    public int getMaxStackSize(ItemStack itemStack) {
        if (this instanceof IArcaneInfiniteChest chest && chest.arcaneforge$isInfinite()) {
            return INFINITE_MAX_STACK;
        }
        return Math.min(99, itemStack.getMaxStackSize());
    }
}
