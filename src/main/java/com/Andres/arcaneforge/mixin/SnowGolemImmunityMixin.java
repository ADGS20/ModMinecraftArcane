package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Corta de raiz la FUENTE del daño ambiental del Golem de Nieve vinculado
// (derretirse en biomas calidos, sensibilidad al agua/lluvia), en vez de
// solo poner el daño resultante en 0 desde el evento LivingDamageEvent.Pre
// (ArcaneSnowGolemHandler.onSnowGolemDamage). LivingEntity.hurtServer() ya
// dispara animacion de golpe (flash rojo), sonido, empuje y el broadcast
// de daño a los clientes ANTES de que el Pre event pueda intervenir — todo
// eso corre con el valor de daño ORIGINAL, sin importar que el Pre event lo
// ponga en 0 despues. Por eso un golem "inmune" seguia viendose golpeado
// cada tick que tocaba agua/lluvia o estaba en un bioma calido, aunque
// nunca perdiera vida de verdad. Interceptando aqui, ni siquiera se llega a
// llamar hurtServer(), asi que no hay nada que animar.
@Mixin(SnowGolem.class)
public abstract class SnowGolemImmunityMixin {

    @Inject(method = "isSensitiveToWater", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$noDrown(CallbackInfoReturnable<Boolean> cir) {
        SnowGolem self = (SnowGolem) (Object) this;
        if (ArcaneGolemUtil.isArcaneGolem(self)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/golem/SnowGolem;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arcaneforge$noMelt(SnowGolem self, ServerLevel level, DamageSource source, float amount) {
        if (ArcaneGolemUtil.isArcaneGolem(self)) {
            return false;
        }
        return self.hurtServer(level, source, amount);
    }
}
