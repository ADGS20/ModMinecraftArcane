package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.ArcaneForgeScreen;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// No @OnlyIn — EventBusSubscriber with Dist.CLIENT handles side-safety.
// No "bus" parameter — 26.1.2 auto-detects by event type.

@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_FORGE_MENU.get(), ArcaneForgeScreen::new);
    }
}
