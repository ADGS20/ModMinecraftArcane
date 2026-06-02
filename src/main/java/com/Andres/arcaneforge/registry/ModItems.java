package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.ArcaneGuideBook;
import com.Andres.arcaneforge.item.BindingWand;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneForge.MODID);

    // Varita de Vinculacion -> usa la clase BindingWand para tener la logica de
    // clic-derecho que vincula cofres a la Mesa Arcana. (Antes era item simple y
    // por eso NO vinculaba nada.) registerItem asigna el ID automaticamente.
    public static final DeferredItem<BindingWand> BINDING_WAND =
            ITEMS.registerItem(
                    "binding_wand",
                    BindingWand::new,
                    props -> props.stacksTo(1).durability(250)
            );

    // Libro Guia Arcano -> usa la clase ArcaneGuideBook para que el clic-derecho
    // entregue un LIBRO ESCRITO de Minecraft con la guia completa.
    public static final DeferredItem<ArcaneGuideBook> ARCANE_GUIDE_BOOK =
            ITEMS.registerItem(
                    "arcane_guide_book",
                    ArcaneGuideBook::new,
                    props -> props.stacksTo(1)
            );
}
