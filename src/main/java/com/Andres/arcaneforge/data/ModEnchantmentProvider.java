package com.Andres.arcaneforge.data;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.registry.ModEnchantments;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.RecipeOutput; // Corrected import
import net.minecraft.data.recipes.RecipeProvider; // Corrected import
import net.minecraft.core.HolderLookup; // Corrected import
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.Identifier; // Corrected import

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentProvider {
    
    public static void registerDataGenerators(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        // Registrar data generators aquí
    }
}
