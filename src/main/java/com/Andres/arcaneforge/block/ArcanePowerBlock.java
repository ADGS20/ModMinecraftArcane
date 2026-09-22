package com.Andres.arcaneforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Todo el bloque (base, pilares, cristales, estrella) se dibuja completo
 * via GeckoLib — no hay modelo Java horneado por detras. Solo el hueso
 * "estrella" tiene animacion; el resto del modelo se queda quieto en su
 * pose de reposo porque la animacion no lo toca.
 */
public class ArcanePowerBlock extends BaseEntityBlock {

    public static final MapCodec<ArcanePowerBlock> CODEC = simpleCodec(ArcanePowerBlock::new);

    public ArcanePowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcanePowerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // En esta version RenderShape solo tiene INVISIBLE/MODEL (ya no
        // existe ENTITYBLOCK_ANIMATED) — INVISIBLE = nada de modelo
        // horneado por blockstate, todo lo dibuja el BlockEntityRenderer.
        return RenderShape.INVISIBLE;
    }
}
