package com.Andres.arcaneforge;

import com.Andres.arcaneforge.event.AxeFortuneHandler;
import com.Andres.arcaneforge.network.ModNetwork;
import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.Andres.arcaneforge.registry.ModBlocks;
import com.Andres.arcaneforge.registry.ModItems;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ArcaneForge.MODID)
public class ArcaneForge {

    public static final String MODID = "arcaneforge";
    public static final String MOD_ID = MODID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneForge(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        // Register network packets
        ModNetwork.register(modEventBus);
        
        // Register event handlers
        modEventBus.register(AxeFortuneHandler.class);
    }
}
