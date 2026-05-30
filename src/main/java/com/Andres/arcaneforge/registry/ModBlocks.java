package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneForge.MODID);

    public static final DeferredBlock<Block> ARCANE_FORGE = BLOCKS.register("arcane_forge",
            // BlockBehaviour.Properties es la forma correcta en NeoForge
            () -> new ArcaneForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, 6.0f).sound(SoundType.ANVIL).noOcclusion()));

    public static final DeferredBlock<Block> ARCANE_PEDESTAL = BLOCKS.register("arcane_pedestal",
            () -> new ArcanePedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 6.0f).sound(SoundType.GLASS).noOcclusion()));
}