package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneForge.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCANEFORGE_TAB =
            CREATIVE_MODE_TABS.register("arcaneforge_tab", () -> 
                CreativeModeTab.builder()
                    .title(Text.translatable("itemGroup.arcaneforge"))
                    .icon(() -> new ItemStack(Items.DIAMOND_SWORD))
                    .displayItems((parameters, output) -> {
                        // Agrega tus items aquí
                        // Ejemplo: output.accept(ModItems.YOUR_ITEM.get());
                    })
                    .build()
            );

    public static void register() {
        // Registration automática
    }
}