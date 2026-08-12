package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.ArcaneForgeScreen;
import com.Andres.arcaneforge.menu.DimensionalBagScreen;
import com.Andres.arcaneforge.menu.MinersSightScreen;
import com.Andres.arcaneforge.miners.MinersSightLogic;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_FORGE_MENU.get(), ArcaneForgeScreen::new);
        event.register(ModMenuTypes.DIMENSIONAL_BAG_MENU.get(), DimensionalBagScreen::new);
    }

    /**
     * Abre el menu de la Vision Minera: agachado + clic derecho al aire con
     * la mano vacia y el casco encantado puesto. No usa RightClickItem
     * porque el encantamiento vive en el CASCO (equipado), no en el item
     * que se tiene en la mano.
     */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (!event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return;
        if (MinersSightLogic.getLevel(player, helmet) <= 0) return;

        boolean enabled = MinersSightLogic.isEnabled(helmet);
        var filter = MinersSightLogic.getFilter(helmet);
        Minecraft.getInstance().setScreen(new MinersSightScreen(enabled, filter));
    }
}