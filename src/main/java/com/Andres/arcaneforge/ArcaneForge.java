package com.Andres.arcaneforge;

import com.Andres.arcaneforge.registry.ModBlocks;
import com.Andres.arcaneforge.registry.ModEnchantments;
import net.minecraft.text.Text;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(ArcaneForge.MODID)
public class ArcaneForge {
    public static final String MODID = "arcaneforge";

    public ArcaneForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
        
        ModBlocks.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Setup común
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Setup del servidor
    }

    @SubscribeEvent
    public void registerDataPacks(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(NeoForgeRegistries.Keys.ENCHANTMENTS, ModEnchantments.ENCHANTMENTS);
    }
}