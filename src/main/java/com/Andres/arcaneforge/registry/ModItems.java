package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.BindingWand;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ArcaneForge.MODID);

    // Block item for the Arcane Forge
    public static final DeferredItem<?> ARCANE_FORGE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ARCANE_FORGE);

    // Block item for the Arcane Pedestal
    public static final DeferredItem<?> ARCANE_PEDESTAL_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ARCANE_PEDESTAL);

    // Binding Wand — links chests to the forge
    public static final DeferredItem<BindingWand> BINDING_WAND =
            ITEMS.registerItem(
                    "binding_wand",
                    BindingWand::new,
                    () -> new Item.Properties().stacksTo(1).durability(250)
            );
}
