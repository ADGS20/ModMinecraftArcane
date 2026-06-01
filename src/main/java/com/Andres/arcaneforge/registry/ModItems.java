package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.mojang.datafixers.TypeRewriteRule;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneForge.MODID);

    public static final DeferredItem<Item> BINDING_WAND = ITEMS.registerSimpleItem("binding_wand");

    public static final DeferredItem<Item> ARCANE_GUIDE_BOOK = ITEMS.registerSimpleItem("arcane_guide_book");
    public static final TypeRewriteRule.Nop ARCANE_FORGE_ITEM = ;
    public static final TypeRewriteRule.Nop ARCANE_PEDESTAL_ITEM = ;
}