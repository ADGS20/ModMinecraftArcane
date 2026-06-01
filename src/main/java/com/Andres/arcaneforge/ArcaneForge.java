package com.Andres.arcaneforge;

import com.Andres.arcaneforge.network.ModNetwork;
import com.Andres.arcaneforge.registry.*;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModDataComponents.REGISTRAR.register(modEventBus);
        ModNetwork.register(modEventBus);
    }
}
