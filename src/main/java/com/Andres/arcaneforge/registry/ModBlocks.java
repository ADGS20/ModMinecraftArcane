package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ArcaneForge.MODID);

    public static final DeferredBlock<ArcaneForgeBlock> ARCANE_FORGE =
            BLOCKS.registerBlock(
                    "arcane_forge",
                    ArcaneForgeBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(5.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 7)
            );

    public static final DeferredBlock<ArcanePedestalBlock> ARCANE_PEDESTAL =
            BLOCKS.registerBlock(
                    "arcane_pedestal",
                    ArcanePedestalBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 5)
            );
}
