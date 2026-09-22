package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.stateless.StatelessAnimationController;
import com.geckolib.animatable.stateless.StatelessGeoBlockEntity;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solo existe para que GeckoLib tenga algo a lo que animar (la estrella
 * flotante) por encima del modelo estatico normal del bloque — el resto del
 * Bloque de Poder (base, pilares, cristales) sigue siendo un modelo Java
 * horneado comun, sin BlockEntity de por medio.
 *
 * "Stateless" porque la animacion no depende de ningun estado del juego
 * (no hay que encenderla/apagarla): gira sola desde que el bloque existe.
 */
public class ArcanePowerBlockEntity extends BlockEntity implements StatelessGeoBlockEntity {

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("Animation_estrella");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArcanePowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_POWER_BLOCK_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        StatelessAnimationController controller = new StatelessAnimationController("spin");
        controller.setCurrentAnimation(SPIN);
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
