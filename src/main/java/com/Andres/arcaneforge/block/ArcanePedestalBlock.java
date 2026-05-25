package com.Andres.arcaneforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ArcanePedestalBlock extends Block {
    public ArcanePedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Verifica si hay un Bloque de Potenciación Arcana válido cerca de la posición dada.
     * Busca en un radio de 3 bloques en todas direcciones.
     */
    public static boolean hasActivePedestalNearby(Level level, BlockPos centerPos) {
        if (level == null) return false;
        int range = 3; // Radio de búsqueda definido en el diseño
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Saltar la posición central

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