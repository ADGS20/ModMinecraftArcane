package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneForge.MODID);

    public static final DeferredBlock<Block> ARCANE_POWER_BLOCK = BLOCKS.register(
            "arcane_power_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_power_block"))))
    );

    public static final DeferredBlock<Block> ARCANE_PEDESTAL = BLOCKS.register(
            "arcane_pedestal",
            () -> new Block(BlockBehaviour.Properties.of()
                    .destroyTime(2.0f)
                    .explosionResistance(5.0f)
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_pedestal"))))
    );

    // Adding ARCANE_FORGE block as it's referenced in ArcaneForgeScreen
    public static final DeferredBlock<Block> ARCANE_FORGE = BLOCKS.register(
            "arcane_forge",
            () -> new Block(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge"))))
    );
}