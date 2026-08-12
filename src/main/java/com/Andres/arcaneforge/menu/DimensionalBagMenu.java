package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.item.BagContainer;
import com.Andres.arcaneforge.item.BagLogic;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Menu del Saco Dimensional: 6 filas completas (54 espacios) de almacen, mas el
 * inventario del jugador. La pagina actual se guarda en el saco. Los botones de
 * pagina viven en la PANTALLA (pestañas a la derecha), no ocupan slots.
 */
public class DimensionalBagMenu extends AbstractContainerMenu {

    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int STORAGE = ROWS * COLS; // 54

    private final Container storage;
    private final ItemStack bag;
    private final int totalPages;
    private int currentPage;

    // ── Constructor servidor ──
    public DimensionalBagMenu(int id, Inventory playerInv, ItemStack bag) {
        super(ModMenuTypes.DIMENSIONAL_BAG_MENU.get(), id);
        this.bag = bag;

        int lvl = Math.max(1, BagLogic.getBindLevel(playerInv.player, bag));
        this.totalPages = BagLogic.pagesForLevel(lvl);

        int page = readPage(bag);
        if (page < 0 || page >= totalPages) page = 0;
        this.currentPage = page;

        this.storage = new BagContainer(bag, currentPage, playerInv.player.level().registryAccess());

        layoutSlots(playerInv);
    }

    // ── Constructor cliente (red) ──
    public DimensionalBagMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenuTypes.DIMENSIONAL_BAG_MENU.get(), id);
        this.bag = ItemStack.EMPTY;
        this.totalPages = Math.max(1, buf.readVarInt());
        this.currentPage = buf.readVarInt();
        // En cliente el contenido real lo sincroniza el servidor; usamos un buffer vacio.
        this.storage = new SimpleContainer(STORAGE);
        layoutSlots(playerInv);
    }

    /** Slot que ignora el limite de 64 del item y permite hasta 1 000 000 000 por slot. */
    private static class UnlimitedSlot extends Slot {
        UnlimitedSlot(Container c, int idx, int x, int y) { super(c, idx, x, y); }
        @Override public int getMaxStackSize() { return BagContainer.MAX_STACK; }
        @Override public int getMaxStackSize(ItemStack stack) { return BagContainer.MAX_STACK; }
    }

    private void layoutSlots(Inventory playerInv) {
        // 6 filas de almacen (54) — slots sin limite de stack
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new UnlimitedSlot(storage, col + row * COLS, 8 + col * 18, 18 + row * 18));
            }
        }
        // Inventario del jugador (3 filas)
        int invY = 18 + ROWS * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
        }
    }

    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPageClient(int page) { this.currentPage = page; }

    private static int readPage(ItemStack bag) {
        CompoundTag tag = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt("bag_page").orElse(0);
    }

    private static void writePage(ItemStack bag, int page) {
        CompoundTag tag = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("bag_page", page);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Llamado en el servidor cuando el jugador pulsa una pestaña. NO reabre la ventana. */
    public void changePage(Player player, int direction) {
        if (bag.isEmpty() || totalPages <= 1) return;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        if (!(storage instanceof BagContainer bagStorage)) return;

        int next = (((currentPage + direction) % totalPages) + totalPages) % totalPages;
        this.currentPage = next;
        writePage(bag, next);

        // Cargar la nueva pagina en los MISMOS slots (sin cerrar la ventana).
        bagStorage.loadPage(next);

        // Refrescar en el cliente cada slot de almacen con su nuevo contenido,
        // usando el mismo metodo probado que usa la forja del mod.
        for (int i = 0; i < STORAGE; i++) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                    this.containerId,
                    this.incrementStateId(),
                    i,
                    this.slots.get(i).getItem()
            ));
        }

        // Avisar al cliente del nuevo numero de pagina (para el texto "X/Y").
        sp.connection.send(new com.Andres.arcaneforge.network.S2CBagPageSync(next, totalPages));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();
            if (index < STORAGE) {
                // de almacen -> inventario jugador
                if (!moveItemStackTo(slotStack, STORAGE, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // de inventario -> almacen
                if (!moveItemStackTo(slotStack, 0, STORAGE, false)) return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (storage instanceof BagContainer) {
            storage.setChanged(); // fuerza guardado
        }
    }
}
