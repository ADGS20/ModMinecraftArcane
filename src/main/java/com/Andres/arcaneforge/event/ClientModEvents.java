package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.ArcaneForgeScreen;
import com.Andres.arcaneforge.menu.DimensionalBagScreen;
import com.Andres.arcaneforge.menu.MinersSightScreen;
import com.Andres.arcaneforge.miners.MinersSightLogic;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    /**
     * Tecla dedicada para abrir el menu de la Vision Minera. El clic derecho
     * al aire (RightClickEmpty) de mas abajo se dejo tambien como atajo,
     * pero NO es fiable por si solo: ese evento vanilla solo se dispara si
     * NINGUN item en la mano consume el clic primero (herramientas, comida,
     * el guante de vinculacion, etc. pueden interceptarlo), asi que si el
     * jugador tenia cualquier cosa en la mano el menu podia no abrirse nunca
     * sin ningun aviso. La tecla siempre funciona sin importar que se tenga
     * en la mano.
     */
    public static final KeyMapping MINERS_SIGHT_KEY = new KeyMapping(
            "key.arcaneforge.miners_sight",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            KeyMapping.Category.MISC
    );

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_FORGE_MENU.get(), ArcaneForgeScreen::new);
        event.register(ModMenuTypes.DIMENSIONAL_BAG_MENU.get(), DimensionalBagScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MINERS_SIGHT_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (MINERS_SIGHT_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) tryOpenMinersSight(mc.player);
        }
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

        tryOpenMinersSight(player);
    }

    private static void tryOpenMinersSight(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        int level = helmet.isEmpty() ? 0 : MinersSightLogic.getLevel(player, helmet);
        ArcaneForge.LOGGER.info("[MINERS-OPEN] intento abrir menu, helmet={} level={}", helmet.getItem(), level);
        if (level <= 0) return;

        boolean enabled = MinersSightLogic.isEnabled(helmet);
        var filter = MinersSightLogic.getFilter(helmet);
        Minecraft.getInstance().setScreen(new MinersSightScreen(enabled, filter));
    }
}
