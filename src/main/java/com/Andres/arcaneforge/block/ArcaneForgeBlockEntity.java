package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.menu.ArcaneForgeMenu;
import com.Andres.arcaneforge.network.S2CSyncPacket;
import com.Andres.arcaneforge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Arcane Forge Block Entity — VERSIÓN AVANZADA para NeoForge 26.1.2.65-beta.
 *
 * Correcciones de API aplicadas:
 * - saveAdditional(ValueOutput) / loadAdditional(ValueInput) — NO CompoundTag
 * - ValueInput: getIntOr("key", default) devuelve int directamente (NO Optional)
 * - ValueOutput: putInt("key", value), store("key", codec, value)
 * - getUpdateTag(HolderLookup.Provider) — requiere parámetro Provider
 * - Enchantments via registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
 * - ItemStack.enchant(Holder<Enchantment>, int) con Holder
 * - Fuerza encantamientos via ItemEnchantments.Mutable (sin restricciones)
 *
 * Características avanzadas:
 * - Hasta 256 cofres vinculados
 * - Sistema de Combustible Mágico (cualquier item)
 * - Niveles de encantamiento de 1 a 10,000
 * - Ignora compatibilidad de encantamientos
 */
public class ArcaneForgeBlockEntity extends BlockEntity implements MenuProvider {

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final List<BlockPos> linkedChests = new ArrayList<>();

    private int cachedBookshelfCount = 0;
    private int cachedMagicFuel = 0;
    private int syncTimer = 0;

    // Datos del lado cliente (establecidos por S2CSyncPacket)
    private int clientLinkedChests = 0;
    private int clientBookshelves = 0;
    private int clientMagicFuel = 0;

    public ArcaneForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_FORGE_BE.get(), pos, blockState);
    }

    // ════════════════════════════════════
    // Acceso al inventario
    // ════════════════════════════════════
    public NonNullList<ItemStack> getItems() { return items; }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    // ════════════════════════════════════
    // Gestión de cofres vinculados
    // ════════════════════════════════════
    public boolean addLinkedChest(BlockPos chestPos) {
        if (linkedChests.size() >= Config.MAX_LINKED_CHESTS) return false;
        for (BlockPos existing : linkedChests) {
            if (existing.equals(chestPos)) return false;
        }
        linkedChests.add(chestPos.immutable());
        setChanged();
        return true;
    }

    public boolean removeLinkedChest(BlockPos chestPos) {
        boolean removed = linkedChests.removeIf(p -> p.equals(chestPos));
        if (removed) setChanged();
        return removed;
    }

    public List<BlockPos> getLinkedChests() { return linkedChests; }
    public int getLinkedChestCount() { return linkedChests.size(); }

    // ════════════════════════════════════
    // Conteo de librerías — radio expandido, sin cap
    // ════════════════════════════════════
    public int countNearbyBookshelves() {
        if (level == null) return 0;
        int count = 0;
        BlockPos center = getBlockPos();
        int rxz = Config.BOOKSHELF_RADIUS_XZ;
        int ry = Config.BOOKSHELF_RADIUS_Y;
        for (int x = -rxz; x <= rxz; x++) {
            for (int y = -ry; y <= ry; y++) {
                for (int z = -rxz; z <= rxz; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockState state = level.getBlockState(center.offset(x, y, z));
                    if (state.is(Blocks.BOOKSHELF)) count++;
                }
            }
        }
        if (Config.MAX_BOOKSHELF_COUNT > 0) {
            return Math.min(count, Config.MAX_BOOKSHELF_COUNT);
        }
        return count;
    }

    public int getNearbyBookshelfCount() { return cachedBookshelfCount; }

    // ════════════════════════════════════
    // SISTEMA DE COMBUSTIBLE MÁGICO
    // ════════════════════════════════════
    public int countMagicFuelInLinkedChests() {
        if (level == null) return 0;
        int total = 0;
        for (BlockPos chestPos : linkedChests) {
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.isEmpty()) continue;
                    int fuelValue = Config.getFuelValue(stack.getItem());
                    if (fuelValue > 0) {
                        total += fuelValue * stack.getCount();
                    }
                }
            }
        }
        return total;
    }

    public int getTotalMagicFuel() { return cachedMagicFuel; }
    public int getTotalLapisCount() { return cachedMagicFuel; } // Alias

    // ════════════════════════════════════
    // Extracción de combustible mágico
    // ════════════════════════════════════
    /**
     * Extrae el combustible mágico necesario de los cofres vinculados.
     * REVISADO: Ahora es más eficiente y asegura el consumo exacto.
     */
    private boolean extractMagicFuel(int requiredPoints) {
        if (level == null) return false;
        int remaining = requiredPoints;

        for (BlockPos chestPos : linkedChests) {
            if (remaining <= 0) break;
            if (!level.isLoaded(chestPos)) continue;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (remaining <= 0) break;

                    ItemStack stack = chest.getItem(i);
                    if (stack.isEmpty()) continue;

                    int fuelValue = Config.getFuelValue(stack.getItem());
                    if (fuelValue <= 0) continue; // Si el ítem no tiene valor configurado, se ignora

                    // Calcular cuántos ítems de este slot necesitamos
                    int itemsNeeded = (int) Math.ceil((double) remaining / fuelValue);
                    int itemsToTake = Math.min(itemsNeeded, stack.getCount());

                    int pointsGained = itemsToTake * fuelValue;

                    // Reducir el stack en el cofre
                    stack.shrink(itemsToTake);
                    if (stack.isEmpty()) {
                        chest.setItem(i, ItemStack.EMPTY);
                    }

                    remaining -= pointsGained;
                    chest.setChanged(); // Marcar el cofre para guardar cambios
                }
            }
        }

        // Si remaining <= 0, significa que pudimos pagar todo el costo
        return remaining <= 0;
    }

    // ════════════════════════════════════
    // ENCANTAMIENTO — SIN RESTRICCIONES
    // Usa ItemEnchantments.Mutable para forzar cualquier encantamiento
    // ════════════════════════════════════
    public boolean tryEnchant(Identifier enchantmentId, int targetLevel, ServerPlayer player) {
        if (level == null || level.isClientSide()) return false;

        ItemStack itemToEnchant = items.get(0);
        if (itemToEnchant.isEmpty()) return false;

        if (targetLevel < 1 || targetLevel > Config.MAX_ENCHANTMENT_LEVEL) return false;

        cachedBookshelfCount = countNearbyBookshelves();
        cachedMagicFuel = countMagicFuelInLinkedChests();

        // Obtener nivel actual del encantamiento
        ItemEnchantments currentEnchants = itemToEnchant.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int currentLevel = 0;
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);
            Optional<Holder.Reference<Enchantment>> optHolder = registry.get(key);
            if (optHolder.isPresent()) {
                currentLevel = currentEnchants.getLevel(optHolder.get());
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.warn("tryEnchant: error obteniendo nivel actual: {}", e.getMessage());
        }

        // Calcular costo basado en nivel actual + nivel objetivo
        int cost = Config.calculateEnchantCost(currentLevel, targetLevel, cachedBookshelfCount);
        if (cachedMagicFuel < cost) return false;

        // Buscar encantamiento en registro data-driven
        Holder<Enchantment> enchHolder = null;
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);
            Optional<Holder.Reference<Enchantment>> optHolder = registry.get(key);
            if (optHolder.isPresent()) {
                enchHolder = optHolder.get();
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.warn("tryEnchant: error buscando {}: {}", enchantmentId, e.getMessage());
        }

        if (enchHolder == null) return false;

        // Extraer combustible
        if (!extractMagicFuel(cost)) return false;

        // FORZAR encantamiento usando Mutable (ignora TODAS las restricciones)
        try {
            forceApplyEnchantment(itemToEnchant, enchHolder, targetLevel);
        } catch (Exception e) {
            // Fallback: intentar método normal
            ArcaneForge.LOGGER.warn("Forzado falló, intentando normal: {}", e.getMessage());
            try {
                itemToEnchant.enchant(enchHolder, targetLevel);
            } catch (Exception e2) {
                ArcaneForge.LOGGER.error("Ambos métodos fallaron", e2);
                return false;
            }
        }

        setChanged();
        cachedMagicFuel = countMagicFuelInLinkedChests();

        ArcaneForge.LOGGER.info("Encantamiento aplicado: {} nivel {} (era {}), costo {} fuel",
                enchantmentId, targetLevel, currentLevel, cost);
        return true;
    }

    /**
     * Fuerza la aplicación de un encantamiento directamente via ItemEnchantments.Mutable.
     * Ignora TODAS las validaciones vanilla (compatibilidad, tipo de item, nivel máximo).
     */
    private void forceApplyEnchantment(ItemStack stack, Holder<Enchantment> holder, int level) {
        ItemEnchantments currentEnchants = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchants);
        mutable.set(holder, level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    // ════════════════════════════════════
    // Datos del lado cliente
    // ════════════════════════════════════
    private boolean clientHasActivePedestal = false;

    public void setClientSyncData(int chests, int bookshelves, int magicFuel) {
        this.clientLinkedChests = chests;
        this.clientBookshelves = bookshelves;
        this.clientMagicFuel = magicFuel;
    }

    public void setClientSyncData(int chests, int bookshelves, int magicFuel, boolean hasActivePedestal) {
        this.clientLinkedChests = chests;
        this.clientBookshelves = bookshelves;
        this.clientMagicFuel = magicFuel;
        this.clientHasActivePedestal = hasActivePedestal;
    }

    public int getClientLinkedChests() { return clientLinkedChests; }
    public int getClientBookshelves() { return clientBookshelves; }
    public int getClientLapisCount() { return clientMagicFuel; }
    public int getClientMagicFuel() { return clientMagicFuel; }
    public boolean hasActivePedestalNearby() { return clientHasActivePedestal; }
    public boolean hasActivePedestal() { return clientHasActivePedestal; }

    // ════════════════════════════════════
    // TICK
    // ════════════════════════════════════
    public static void tick(Level level, BlockPos pos, BlockState state,
                            ArcaneForgeBlockEntity be) {
        if (level.isClientSide()) return;

        be.syncTimer++;
        if (be.syncTimer >= Config.SYNC_INTERVAL_TICKS) {
            be.syncTimer = 0;
            be.validateLinkedChests();
            be.cachedBookshelfCount = be.countNearbyBookshelves();
            be.cachedMagicFuel = be.countMagicFuelInLinkedChests();

            // Verificar si hay pedestal arcano activo cerca
            boolean hasActivePedestal = ArcanePedestalBlock.hasActivePedestalNearby(level, pos);

            if (level instanceof ServerLevel serverLevel) {
                S2CSyncPacket syncPacket = new S2CSyncPacket(
                        pos, be.linkedChests.size(),
                        be.cachedBookshelfCount, be.cachedMagicFuel, hasActivePedestal);
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.containerMenu instanceof ArcaneForgeMenu forgeMenu) {
                        if (forgeMenu.getBlockEntity() == be) {
                            PacketDistributor.sendToPlayer(player, syncPacket);
                        }
                    }
                }
            }
        }
    }

    private void validateLinkedChests() {
        if (level == null) return;
        boolean changed = linkedChests.removeIf(chestPos -> {
            if (!level.isLoaded(chestPos)) return false;
            BlockEntity be = level.getBlockEntity(chestPos);
            return !(be instanceof ChestBlockEntity);
        });
        if (changed) setChanged();
    }

    // ════════════════════════════════════
    // SERIALIZACIÓN — ValueOutput / ValueInput API (26.1.2)
    //
    // En 26.1.2:
    //   ValueOutput: putInt("key", val), store("key", codec, val)
    //   ValueInput:  getIntOr("key", default) → devuelve int (NO Optional!)
    //                read("key", codec) → devuelve Optional<T>
    // ════════════════════════════════════
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // Guardar item del slot
        if (!items.get(0).isEmpty()) {
            output.store("slot_0", ItemStack.CODEC, items.get(0));
        }

        // Guardar cofres vinculados
        output.putInt("chest_count", linkedChests.size());
        for (int i = 0; i < linkedChests.size(); i++) {
            BlockPos cp = linkedChests.get(i);
            output.putInt("chest_" + i + "_x", cp.getX());
            output.putInt("chest_" + i + "_y", cp.getY());
            output.putInt("chest_" + i + "_z", cp.getZ());
        }

        // Guardar valores cacheados
        output.putInt("bookshelf_count", cachedBookshelfCount);
        output.putInt("magic_fuel", cachedMagicFuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Cargar item del slot — read() devuelve Optional<T>
        items.set(0, input.read("slot_0", ItemStack.CODEC).orElse(ItemStack.EMPTY));

        // Cargar cofres vinculados — getIntOr devuelve int directamente
        linkedChests.clear();
        int chestCount = input.getIntOr("chest_count", 0);
        for (int i = 0; i < chestCount; i++) {
            int cx = input.getIntOr("chest_" + i + "_x", 0);
            int cy = input.getIntOr("chest_" + i + "_y", 0);
            int cz = input.getIntOr("chest_" + i + "_z", 0);
            linkedChests.add(new BlockPos(cx, cy, cz));
        }

        // Cargar valores cacheados
        cachedBookshelfCount = input.getIntOr("bookshelf_count", 0);
        cachedMagicFuel = input.getIntOr("magic_fuel", 0);
        // Compatibilidad con versiones anteriores
        if (cachedMagicFuel == 0) {
            cachedMagicFuel = input.getIntOr("lapis_count", 0);
        }
    }

    // ── getUpdateTag requiere HolderLookup.Provider en 26.1 ──
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);

        tag.putInt("chest_count", linkedChests.size());
        for (int i = 0; i < linkedChests.size(); i++) {
            BlockPos cp = linkedChests.get(i);
            tag.putInt("chest_" + i + "_x", cp.getX());
            tag.putInt("chest_" + i + "_y", cp.getY());
            tag.putInt("chest_" + i + "_z", cp.getZ());
        }
        tag.putInt("bookshelf_count", cachedBookshelfCount);
        tag.putInt("magic_fuel", cachedMagicFuel);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ════════════════════════════════════
    // MenuProvider
    // ════════════════════════════════════
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arcaneforge.arcane_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory,
                                            Player player) {
        return new ArcaneForgeMenu(containerId, playerInventory, this);
    }
}
