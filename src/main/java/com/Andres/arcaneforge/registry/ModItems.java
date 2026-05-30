package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ArcaneForge.MODID);
    
    // Agrega tus items aquí
    // public static final DeferredHolder<Item, Item> YOUR_ITEM = 
    //         ITEMS.register("your_item", () -> new Item(new Item.Settings()));
    
    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}