package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.BagLogic;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * VÍNCULO DIMENSIONAL — abre el Saco Dimensional (menu propio con pestañas).
 * Clic derecho en el aire con el saco encantado.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class DimensionalBindHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack bag = event.getItemStack();
        int lvl = BagLogic.getBindLevel(player, bag);
        if (lvl <= 0) return;

        int totalPages = BagLogic.pagesForLevel(lvl);

        // Pagina guardada y validada.
        CompoundTag tag = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int page = tag.getInt("bag_page").orElse(0);
        if (page < 0 || page >= totalPages) {
            page = 0;
            tag.putInt("bag_page", 0);
            bag.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        final int currentPage = page;

        // Sonido magico.
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDER_CHEST_OPEN,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
        }

        SimpleMenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new DimensionalBagMenu(id, inv, bag),
                Component.literal("§5Saco Dimensional §7(" + (currentPage + 1) + "/" + totalPages + ")"));

        final boolean magnetEnabled = BagLogic.isMagnetEnabled(bag);
        var openResult = player.openMenu(provider, buf -> {
            buf.writeVarInt(totalPages);
            buf.writeVarInt(currentPage);
            buf.writeBoolean(magnetEnabled);
        });
        ArcaneForge.LOGGER.info("[BAG-OPEN] openMenu result={} containerMenu={}",
                openResult, player.containerMenu == null ? "null" : player.containerMenu.getClass().getSimpleName());

        // El sync automatico de "menu recien abierto" tampoco es confiable en
        // este build (igual que el broadcast ambiental de cambios, que ya
        // necesito empujar a mano en changePage()/syncStorageToClient()). Sin
        // esto, el cliente se queda mostrando el ultimo valor que tenia
        // cacheado de una sesion anterior en vez del contenido recien cargado
        // desde el NBT de la bolsa.
        if (player.containerMenu instanceof DimensionalBagMenu bagMenu) {
            ArcaneForge.LOGGER.info("[BAG-OPEN] llamando syncStorageToClient, containerId={}", bagMenu.containerId);
            // Se envia en el MISMO tick que el paquete de apertura de pantalla.
            // Por si el cliente todavia no proceso ese paquete y descarta estos
            // (condicion de carrera), se reenvia una segunda vez un tick despues.
            bagMenu.syncStorageToClient(player);
            if (player.level() instanceof ServerLevel sl2) {
                sl2.getServer().execute(() -> {
                    if (player.containerMenu == bagMenu) {
                        ArcaneForge.LOGGER.info("[BAG-OPEN] reenvio diferido de syncStorageToClient");
                        bagMenu.syncStorageToClient(player);
                    }
                });
            }
        } else {
            ArcaneForge.LOGGER.info("[BAG-OPEN] player.containerMenu NO es DimensionalBagMenu, no se sincroniza");
        }

        event.setCanceled(true);
    }
}
