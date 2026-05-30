package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneForge.MODID);

    public static final Supplier<CreativeModeTab> ARCANE_FORGE_TAB = CREATIVE_TABS.register("arcaneforge_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.arcaneforge"))
                    .icon(() -> new ItemStack(ModBlocks.ARCANE_FORGE.get()))
                    .displayItems((parameters, output) -> {
                        // Agregar aquí todos tus ítems para que aparezcan en tu pestaña
                        output.accept(ModBlocks.ARCANE_FORGE.get());
                        output.accept(ModBlocks.ARCANE_PEDESTAL.get());
                        output.accept(ModItems.BINDING_WAND.get());
                        output.accept(ModItems.ARCANE_GUIDE_BOOK.get());
                    })
                    .build()
    );
}