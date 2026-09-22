package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.ArcaneGuideBook;
import com.Andres.arcaneforge.item.BindingWand;
import com.Andres.arcaneforge.item.GolemBindingRod;
import com.Andres.arcaneforge.item.LunarFragmentItem;
import com.Andres.arcaneforge.item.SnowGolemBindingRod;
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

    // Cetro Arcano del Golem -> se encanta en la Forja y se rompe al vincular
    // TODOS sus encantamientos a un golem de hierro con clic derecho.
    public static final DeferredItem<GolemBindingRod> GOLEM_BINDING_ROD =
            ITEMS.registerItem(
                    "golem_binding_rod",
                    GolemBindingRod::new,
                    props -> props.stacksTo(1)
            );

    // Cetro Arcano del Golem de Nieve -> hermano del anterior, mismo flujo
    // pero solo actua sobre golems de nieve (propia textura/bonos).
    public static final DeferredItem<SnowGolemBindingRod> SNOW_GOLEM_BINDING_ROD =
            ITEMS.registerItem(
                    "snow_golem_binding_rod",
                    SnowGolemBindingRod::new,
                    props -> props.stacksTo(1)
            );

    // Nucleo Arcano de Golem -> ingrediente puro (sin logica propia), solo lo
    // vende el Aldeano Minero en su ultimo tramo de confianza, y es el
    // ingrediente unico que exige la receta del Cetro Arcano del Golem.
    public static final DeferredItem<Item> ARCANE_GOLEM_CORE =
            ITEMS.registerItem(
                    "arcane_golem_core",
                    Item::new,
                    props -> props.stacksTo(64)
            );

    // Fragmento Lunar -> consumible que adelanta la proxima Luna Oscura/Roja
    // (ver moon/ArcaneMoonData y event/ArcaneMoonHandler).
    public static final DeferredItem<LunarFragmentItem> LUNAR_FRAGMENT =
            ITEMS.registerItem(
                    "lunar_fragment",
                    LunarFragmentItem::new,
                    props -> props.stacksTo(16)
            );

    // Nucleo Arcano del Generador -> se encanta en la Forja (item "universal",
    // igual que el Cetro del Golem) y se inserta en el Generador Arcano de
    // Golems; sin logica propia, toda la interaccion vive en el bloque.
    public static final DeferredItem<Item> GENERATOR_CORE =
            ITEMS.registerItem(
                    "generator_core",
                    Item::new,
                    props -> props.stacksTo(1)
            );
}
