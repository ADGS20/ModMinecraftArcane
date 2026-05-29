package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.registry.ModBlocks;
import com.Andres.arcaneforge.registry.ModMenuTypes;
import com.Andres.arcaneforge.network.S2CSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ArcaneForgeMenu extends AbstractContainerMenu {

    private final ArcaneForgeBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Constructor del servidor
    public ArcaneForgeMenu(int containerId, Inventory playerInventory, ArcaneForgeBlockEntity be) {
        super(ModMenuTypes.ARCANE_FORGE_MENU.get(), containerId);
        this.blockEntity = be;
        this.levelAccess = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // Slot de item — centro de la GUI
        addSlot(new Slot(
                new net.minecraft.world.SimpleContainer(1) {
                    @Override
                    public ItemStack getItem(int slot) {
                        return be.getItem(slot);
                    }
                    @Override
                    public void setItem(int slot, ItemStack stack) {
                        be.setItem(slot, stack);
                    }
                    @Override
                    public int getMaxStackSize() { return 1; }
                    @Override
                    public boolean canPlaceItem(int slot, ItemStack stack) { return true; }
                },
                0, 80, 35
        ));

        // Inventario del jugador (3 filas × 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // 🔄 SINCRONIZACIÓN INMEDIATA (Sugerencia 1): Al abrir el bloque, enviamos los datos actualizados
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            be.validateLinkedChests();
            int currentFuel = be.countMagicFuelInLinkedChests();
            int bookshelves = be.countNearbyBookshelves();
            boolean hasPedestal = com.Andres.arcaneforge.block.ArcanePedestalBlock.hasActivePedestalNearby(be.getLevel(), be.getBlockPos());
            ArcaneForgeBlockEntity.FuelBreakdown bd = be.computeFuelBreakdown();

            serverPlayer.connection.send(new S2CSyncPacket(
                    be.getBlockPos(), be.getLinkedChestCount(), bookshelves, currentFuel, hasPedestal,
                    bd.common, bd.uncommon, bd.rare, bd.epic, bd.legendary
            ));
        }
    }

    // Constructor de red (cliente)
    public ArcaneForgeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    private static ArcaneForgeBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArcaneForgeBlockEntity forgeBE) return forgeBE;
        throw new IllegalStateException("No ArcaneForgeBlockEntity at received position.");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();
            if (index == 0) {
                if (!moveItemStackTo(slotStack, 1, 37, true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.ARCANE_FORGE.get());
    }

    public ArcaneForgeBlockEntity getBlockEntity() { return blockEntity; }
}