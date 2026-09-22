package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cofre Infinito (encantamiento almacen_infinito): mientras arcaneforge$infinite
 * sea true, cada slot puede guardar mucho mas de 64/99 items (ver
 * InfiniteChestCapacityMixin, que sube el limite real). El problema es guardar
 * eso en disco: igual que el ItemStack.CODEC que usa ContainerHelper.saveAllItems
 * (ver ItemEntityRealCountMixin para la misma explicacion), el conteo se
 * recorta/falla fuera del rango 1..99. Usamos el mismo truco que ya usa
 * BagContainer para sus paginas: antes de guardar, cualquier slot con mas de 99
 * se baja a 1 (para que el guardado normal no falle) y el conteo real se
 * guarda aparte, en claves NBT propias "ArcaneForgeRealCount<slot>"; justo
 * despues de guardar, se restaura el conteo real en memoria (el jugador nunca
 * ve el recorte). Al cargar, si esas claves existen, se reaplican.
 */
@Mixin(ChestBlockEntity.class)
public abstract class InfiniteChestBlockEntityMixin implements IArcaneInfiniteChest {

    private static final String INFINITE_KEY = "ArcaneForgeInfinite";
    private static final String REAL_COUNT_PREFIX = "ArcaneForgeRealCount";

    @Shadow
    private NonNullList<ItemStack> items;

    @Unique
    private boolean arcaneforge$infinite = false;

    @Unique
    private int[] arcaneforge$pendingRestore;

    @Override
    public void arcaneforge$setInfinite(boolean value) {
        this.arcaneforge$infinite = value;
    }

    @Override
    public boolean arcaneforge$isInfinite() {
        return this.arcaneforge$infinite;
    }

    @Inject(method = "saveAdditional", at = @At("HEAD"))
    private void arcaneforge$clampBeforeSave(ValueOutput output, CallbackInfo ci) {
        output.putBoolean(INFINITE_KEY, this.arcaneforge$infinite);
        if (!this.arcaneforge$infinite) return;

        int size = this.items.size();
        int[] restore = new int[size];
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.items.get(i);
            int count = stack.getCount();
            if (count > 99) {
                output.putInt(REAL_COUNT_PREFIX + i, count);
                restore[i] = count;
                stack.setCount(1);
            } else {
                restore[i] = -1;
            }
        }
        this.arcaneforge$pendingRestore = restore;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void arcaneforge$restoreAfterSave(ValueOutput output, CallbackInfo ci) {
        int[] restore = this.arcaneforge$pendingRestore;
        if (restore == null) return;
        for (int i = 0; i < restore.length; i++) {
            if (restore[i] > 0) {
                this.items.get(i).setCount(restore[i]);
            }
        }
        this.arcaneforge$pendingRestore = null;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void arcaneforge$loadRealCounts(ValueInput input, CallbackInfo ci) {
        this.arcaneforge$infinite = input.getBooleanOr(INFINITE_KEY, false);
        if (!this.arcaneforge$infinite) return;

        int size = this.items.size();
        for (int i = 0; i < size; i++) {
            int realCount = input.getIntOr(REAL_COUNT_PREFIX + i, -1);
            if (realCount > 99) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty()) {
                    stack.setCount(realCount);
                }
            }
        }
    }
}
