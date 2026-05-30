package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.BindingWand;
import com.Andres.arcaneforge.item.ArcaneGuideBook;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // Usamos el registro oficial de NeoForge
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneForge.MODID);

    public static final DeferredItem<Item> ARCANE_FORGE = ITEMS.register("arcane_forge",
            () -> new BlockItem(ModBlocks.ARCANE_FORGE.get(), new Item.Properties()));

    public static final DeferredItem<Item> ARCANE_PEDESTAL = ITEMS.register("arcane_pedestal",
            () -> new BlockItem(ModBlocks.ARCANE_PEDESTAL.get(), new Item.Properties()));

    // Adding ARCANE_POWER_BLOCK item
    public static final DeferredItem<Item> ARCANE_POWER_BLOCK = ITEMS.register("arcane_power_block",
            () -> new BlockItem(ModBlocks.ARCANE_POWER_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<Item> BINDING_WAND = ITEMS.register("binding_wand",
            () -> new BindingWand(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ARCANE_GUIDE_BOOK = ITEMS.register("arcane_guide_book",
            () -> new ArcaneGuideBook(new Item.Properties().stacksTo(1)));
}