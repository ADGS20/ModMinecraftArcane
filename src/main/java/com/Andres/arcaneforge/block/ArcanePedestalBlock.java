package com.Andres.arcaneforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class ArcanePedestalBlock extends Block {

    public ArcanePedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static boolean hasActivePedestalNearby(Level level, BlockPos centerPos) {
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof ArcanePedestalBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}