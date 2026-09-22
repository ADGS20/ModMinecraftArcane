package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.GhostStrideUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player.getMovementEmission() decide si los pasos generan sonido
 * (Entity.MovementEmission.emitsSounds) y vibracion para sensores sculk
 * (emitsEvents). Vanilla ya devuelve NONE mientras el jugador esta agachado;
 * con Zancada Fantasma puesto forzamos NONE siempre, sin depender de si
 * esta agachado o no.
 */
@Mixin(Player.class)
public abstract class GhostStrideEmissionMixin {

    @Inject(method = "getMovementEmission", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$silentStride(CallbackInfoReturnable<Entity.MovementEmission> cir) {
        Player self = (Player) (Object) this;
        if (GhostStrideUtil.isActive(self)) {
            cir.setReturnValue(Entity.MovementEmission.NONE);
        }
    }
}
