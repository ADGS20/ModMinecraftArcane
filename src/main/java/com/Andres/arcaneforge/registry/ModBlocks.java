package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneForge.MODID);

    public static final DeferredBlock<Block> ARCANE_FORGE = BLOCKS.registerSimpleBlock(
            "arcane_forge",
            props -> props
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(net.minecraft.resources.ResourceKey.create(BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge")))
    );
}