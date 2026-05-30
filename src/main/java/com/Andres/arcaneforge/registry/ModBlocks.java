package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArcaneForge.MODID);
    
    public static final DeferredHolder<Block, Block> ARCANE_FORGE_BLOCK = 
            BLOCKS.register("arcane_forge", () -> new ArcaneForgeBlock(
                AbstractBlock.Settings.create()
                    .strength(3.0f)
                    .requiresTool()
            ));
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}