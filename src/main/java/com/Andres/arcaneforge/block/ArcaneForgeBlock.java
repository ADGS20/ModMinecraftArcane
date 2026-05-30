package com.Andres.arcaneforge.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;

public class ArcaneForgeBlock extends Block {
    public static final MapCodec<ArcaneForgeBlock> CODEC = simpleCodec(ArcaneForgeBlock::new);

    public ArcaneForgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }
}