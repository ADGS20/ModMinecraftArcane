package com.Andres.arcaneforge.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemStack.CODEC (usado por ItemEntity.addAdditionalSaveData/readAdditionalSaveData
 * para guardar el item tirado en el suelo) codifica "count" con un rango fijo de
 * 1..99 — CUALQUIER cantidad fuera de ese rango falla al codificar. Eso significa
 * que si sacas del Saco Dimensional (o de un futuro Cofre Infinito) un stack de,
 * digamos, 256 esmeraldas y lo tiras al piso (Q/Ctrl+Q), el ItemEntity en el mundo
 * SI tiene las 256 en memoria al momento de tirarlo — pero en el instante en que
 * ese entity se guarda de verdad (cambio de chunk, guardado del mundo, etc.), el
 * conteo real se pierde en silencio y el item puede desaparecer o quedar roto al
 * volver a cargar.
 *
 * El Saco Dimensional ya esquiva este mismo problema para sus propios slots
 * (ver BagContainer.save()/loadFromBag(): codifica el stack con cantidad 1 y
 * guarda la cantidad real aparte, en un campo NBT propio sin limite). Aqui
 * aplicamos exactamente el mismo truco, pero al ItemEntity que vive en el mundo:
 * justo antes de que vainilla guarde, si el conteo real pasa de 99, lo bajamos a
 * 1 (para que ItemStack.CODEC lo codifique sin fallar) y guardamos el conteo real
 * en una etiqueta NBT propia; justo despues de guardar, restauramos el conteo real
 * en memoria (el jugador nunca ve el recorte). Al cargar, si esa etiqueta esta
 * presente, sobreescribimos el conteo del stack ya cargado con el valor real.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityRealCountMixin {

    private static final String REAL_COUNT_KEY = "ArcaneForgeRealCount";

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public abstract void setItem(ItemStack itemStack);

    @Unique
    private int arcaneforge$pendingRestoreCount = -1;

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void arcaneforge$clampBeforeSave(ValueOutput output, CallbackInfo ci) {
        ItemStack current = this.getItem();
        int realCount = current.getCount();
        if (realCount > 99) {
            output.putInt(REAL_COUNT_KEY, realCount);
            this.arcaneforge$pendingRestoreCount = realCount;
            this.setItem(current.copyWithCount(1));
        } else {
            this.arcaneforge$pendingRestoreCount = -1;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void arcaneforge$restoreAfterSave(ValueOutput output, CallbackInfo ci) {
        if (this.arcaneforge$pendingRestoreCount > 0) {
            this.setItem(this.getItem().copyWithCount(this.arcaneforge$pendingRestoreCount));
            this.arcaneforge$pendingRestoreCount = -1;
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void arcaneforge$restoreRealCountOnLoad(ValueInput input, CallbackInfo ci) {
        int realCount = input.getIntOr(REAL_COUNT_KEY, -1);
        if (realCount > 99) {
            ItemStack current = this.getItem();
            if (!current.isEmpty()) {
                this.setItem(current.copyWithCount(realCount));
            }
        }
    }
}
