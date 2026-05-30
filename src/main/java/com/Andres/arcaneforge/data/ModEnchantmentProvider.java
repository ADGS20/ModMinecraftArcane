package com.Andres.arcaneforge.data;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.registry.ModEnchantments;
import net.minecraft.data.DataProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentProvider {
    
    public static void registerDataGenerators(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        // Registrar data generators aquí
    }
}