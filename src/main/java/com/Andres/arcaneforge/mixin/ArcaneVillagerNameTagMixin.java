package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.GhostStrideUtil;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * shouldShowName() esta declarado en EntityRenderer, pero LivingEntityRenderer
 * (la clase real detras de PlayerRenderer y MobRenderer, o sea CUALQUIER
 * jugador o mob con cuerpo) lo REESCRIBE por completo sin llamar a super.
 * Inyectar en EntityRenderer.shouldShowName no sirve de nada para aldeanos
 * ni jugadores: en runtime siempre corre la version de LivingEntityRenderer.
 * Por eso hay que inyectar aqui, donde de verdad se ejecuta.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class ArcaneVillagerNameTagMixin {

    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcaneforge$hideOurNameTag(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (entity.hasCustomName()
                && entity.getCustomName() != null
                && entity.getCustomName().getString().startsWith("Aldeano Arcano")) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Zancada Fantasma (pantalones): oculta el nametag del jugador que lo
     * lleva puesto para el resto de jugadores, siempre que esta activo.
     * Mismo punto de entrada que el aldeano arcano de arriba, gate distinto.
     */
    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcaneforge$hideGhostStridePlayerNameTag(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (GhostStrideUtil.isActive(entity)) {
            cir.setReturnValue(false);
        }
    }
}
