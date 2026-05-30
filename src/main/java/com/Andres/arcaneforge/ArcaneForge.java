package com.Andres.arcaneforge;

import com.Andres.arcaneforge.registry.ModBlocks;
import com.Andres.arcaneforge.registry.ModCreativeTab;
import com.Andres.arcaneforge.registry.ModEnchantments;
import com.Andres.arcaneforge.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ArcaneForge.MODID)
public class ArcaneForge {
    public static final String MODID = "arcaneforge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public ArcaneForge(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
    }
}