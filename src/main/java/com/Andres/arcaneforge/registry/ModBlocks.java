package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneForge.MODID);

    // El bloque del ArcaneForge — usa ArcaneForgeBlock para tener GUI y BlockEntity
    public static final DeferredBlock<ArcaneForgeBlock> ARCANE_FORGE = BLOCKS.register(
            "arcane_forge",
            () -> new ArcaneForgeBlock(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge"))))
    );

    // El pedestal — usa ArcanePedestalBlock para su lógica de detección
    public static final DeferredBlock<ArcanePedestalBlock> ARCANE_PEDESTAL = BLOCKS.register(
            "arcane_pedestal",
            () -> new ArcanePedestalBlock(BlockBehaviour.Properties.of()
                    .destroyTime(2.0f)
                    .explosionResistance(5.0f)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_pedestal"))))
    );

    // El bloque de poder arcano — bloque genérico decorativo
    public static final DeferredBlock<Block> ARCANE_POWER_BLOCK = BLOCKS.register(
            "arcane_power_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_power_block"))))
    );

    // BlockItems — necesarios para que los bloques existan como items en el inventario
    public static final DeferredItem<BlockItem> ARCANE_FORGE_ITEM =
            ModItems.ITEMS.register("arcane_forge",
                    () -> new BlockItem(ARCANE_FORGE.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge")))));

    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_ITEM =
            ModItems.ITEMS.register("arcane_pedestal",
                    () -> new BlockItem(ARCANE_PEDESTAL.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_pedestal")))));

    public static final DeferredItem<BlockItem> ARCANE_POWER_BLOCK_ITEM =
            ModItems.ITEMS.register("arcane_power_block",
                    () -> new BlockItem(ARCANE_POWER_BLOCK.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_power_block")))));
}