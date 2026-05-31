package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ArcaneForge.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCANEFORGE_TAB =
            CREATIVE_MODE_TABS.register("arcaneforge_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.arcaneforge"))
                    .icon(() -> new ItemStack(Items.DIAMOND_SWORD))
                    .displayItems((params, output) -> {
                        output.accept(Items.DIAMOND_SWORD);
                        output.accept(ModItems.BINDING_WAND.get());
                        output.accept(ModItems.ARCANE_GUIDE_BOOK.get());
                    })
                    .build());
}