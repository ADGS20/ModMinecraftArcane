package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.GhostStrideUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity.isIgnoringBlockTriggers() es el mismo gate que usan tanto
 * BasePressurePlateBlock.getEntityCount como TripWireBlock.updateState para
 * decidir si una entidad activa placas de presion e hilos trampa. Con
 * Zancada Fantasma puesto, la entidad simplemente deja de contar para
 * ambos, sin tocar nada mas de esos bloques.
 */
@Mixin(Entity.class)
public abstract class GhostStrideBlockTriggerMixin {

    @Inject(method = "isIgnoringBlockTriggers", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$ignoreTriggers(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && GhostStrideUtil.isActive(living)) {
            cir.setReturnValue(true);
        }
    }
}
