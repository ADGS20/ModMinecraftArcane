package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneForge.MODID);

    public static final net.neoforged.neoforge.registries.DeferredItem<Item> TEST_ITEM =
            ITEMS.registerSimpleItem("test_item");
}