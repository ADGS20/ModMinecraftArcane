package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vainilla excluye a proposito al Creeper de canAttack() (IronGolem.java:
// "return target.is(EntityType.CREEPER) ? false : super.canAttack(target);")
// para que un golem normal no se autodetone peleando cerca de uno. Un Golem
// Arcano puede llevar encantamientos de proteccion (Coraza Arcana, Proteccion
// del Vacio) y se auto-repara al matar (ver ArcaneGolemHandler.
// onGolemKillAutoRepair), asi que ese riesgo ya no aplica igual — el usuario
// quiere que SI puedan atacar creepers. El objetivo en si lo consigue
// ArcaneGolemHandler.onGolemHuntCreeper (vainilla nunca se lo propone solo).
@Mixin(IronGolem.class)
public abstract class ArcaneGolemCreeperTargetMixin {

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void arcaneforge$allowCreeperTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        IronGolem self = (IronGolem) (Object) this;
        if (target instanceof Creeper && ArcaneGolemUtil.isArcaneGolem(self)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
