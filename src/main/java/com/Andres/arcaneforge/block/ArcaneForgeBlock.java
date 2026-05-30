package com.Andres.arcaneforge.block;

import net.minecraft.world.level.block.Block; // Corrected import
import net.minecraft.world.level.block.state.BlockState; // Corrected import
import net.minecraft.world.level.block.state.properties.Property; // Corrected import
import net.minecraft.world.level.block.state.StateDefinition; // Corrected import
import net.minecraft.world.level.block.state.BlockBehaviour; // Corrected import
import net.minecraft.world.item.context.BlockPlaceContext; // Corrected import
import net.minecraft.core.Direction; // Corrected import
import net.minecraft.core.BlockPos; // Corrected import
import net.minecraft.world.level.LevelAccessor; // Corrected import
import net.minecraft.world.phys.shapes.VoxelShape; // Corrected import
import net.minecraft.world.phys.shapes.Shapes; // Corrected import
import net.minecraft.resources.Identifier; // Corrected import
import com.mojang.serialization.MapCodec; // Corrected import

public class ArcaneForgeBlock extends Block {
    
    public ArcaneForgeBlock(BlockBehaviour.Properties settings) { // Corrected constructor parameter
        super(settings);
    }
    
    @Override
    protected MapCodec<? extends Block> codec() { // Corrected method name to codec()
        return MapCodec.unit(this::new);
    }
}