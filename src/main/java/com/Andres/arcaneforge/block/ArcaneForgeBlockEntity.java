package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.menu.ArcaneForgeMenu;
import com.Andres.arcaneforge.network.S2CSyncPacket;
import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.Andres.arcaneforge.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArcaneForgeBlockEntity extends BlockEntity implements MenuProvider {

    public static final Map<Item, Integer> MATERIAL_FUEL_VALUES = new HashMap<>();

    static {
        MATERIAL_FUEL_VALUES.put(Items.COAL, 1);
        MATERIAL_FUEL_VALUES.put(Items.CHARCOAL, 1);
        MATERIAL_FUEL_VALUES.put(Items.STONE, 1);
        MATERIAL_FUEL_VALUES.put(Items.COBBLESTONE, 1);
        MATERIAL_FUEL_VALUES.put(Items.GRAVEL, 1);
        MATERIAL_FUEL_VALUES.put(Items.SAND, 1);
        MATERIAL_FUEL_VALUES.put(Items.BONE, 2);
        MATERIAL_FUEL_VALUES.put(Items.ROTTEN_FLESH, 1);
        MATERIAL_FUEL_VALUES.put(Items.STRING, 2);
        MATERIAL_FUEL_VALUES.put(Items.GUNPOWDER, 2);
        MATERIAL_FUEL_VALUES.put(Items.SPIDER_EYE, 2);
        MATERIAL_FUEL_VALUES.put(Items.FEATHER, 2);
        MATERIAL_FUEL_VALUES.put(Items.WHEAT, 1);
        MATERIAL_FUEL_VALUES.put(Items.LEATHER, 2);
        MATERIAL_FUEL_VALUES.put(Items.FLINT, 2);
        MATERIAL_FUEL_VALUES.put(Items.SUGAR_CANE, 1);
        MATERIAL_FUEL_VALUES.put(Items.PAPER, 1);

        MATERIAL_FUEL_VALUES.put(Items.IRON_INGOT, 10);
        MATERIAL_FUEL_VALUES.put(Items.IRON_NUGGET, 2);
        MATERIAL_FUEL_VALUES.put(Items.COPPER_INGOT, 8);
        MATERIAL_FUEL_VALUES.put(Items.LAPIS_LAZULI, 15);
        MATERIAL_FUEL_VALUES.put(Items.LAPIS_BLOCK, 135);
        MATERIAL_FUEL_VALUES.put(Items.REDSTONE, 10);
        MATERIAL_FUEL_VALUES.put(Items.GLOWSTONE_DUST, 12);
        MATERIAL_FUEL_VALUES.put(Items.QUARTZ, 10);
        MATERIAL_FUEL_VALUES.put(Items.ENDER_PEARL, 25);
        MATERIAL_FUEL_VALUES.put(Items.BLAZE_POWDER, 20);
        MATERIAL_FUEL_VALUES.put(Items.GOLD_NUGGET, 5);
        MATERIAL_FUEL_VALUES.put(Items.GOLD_INGOT, 20);
        MATERIAL_FUEL_VALUES.put(Items.BOOK, 12);
        MATERIAL_FUEL_VALUES.put(Items.SLIME_BALL, 15);
        MATERIAL_FUEL_VALUES.put(Items.MAGMA_CREAM, 18);
        MATERIAL_FUEL_VALUES.put(Items.GHAST_TEAR, 20);

        MATERIAL_FUEL_VALUES.put(Items.DIAMOND, 80);
        MATERIAL_FUEL_VALUES.put(Items.EMERALD, 60);
        MATERIAL_FUEL_VALUES.put(Items.AMETHYST_SHARD, 50);
        MATERIAL_FUEL_VALUES.put(Items.ECHO_SHARD, 100);
        MATERIAL_FUEL_VALUES.put(Items.PRISMARINE_SHARD, 50);
        MATERIAL_FUEL_VALUES.put(Items.PRISMARINE_CRYSTALS, 55);
        MATERIAL_FUEL_VALUES.put(Items.SHULKER_SHELL, 90);
        MATERIAL_FUEL_VALUES.put(Items.RABBIT_FOOT, 55);
        MATERIAL_FUEL_VALUES.put(Items.FERMENTED_SPIDER_EYE, 50);
        MATERIAL_FUEL_VALUES.put(Items.BLAZE_ROD, 60);
        MATERIAL_FUEL_VALUES.put(Items.HEART_OF_THE_SEA, 90);
        MATERIAL_FUEL_VALUES.put(Items.TURTLE_SCUTE, 55);

        MATERIAL_FUEL_VALUES.put(Items.NETHERITE_SCRAP, 250);
        MATERIAL_FUEL_VALUES.put(Items.NETHERITE_INGOT, 400);
        MATERIAL_FUEL_VALUES.put(Items.ELYTRA, 350);
        MATERIAL_FUEL_VALUES.put(Items.DRAGON_BREATH, 300);
        MATERIAL_FUEL_VALUES.put(Items.END_CRYSTAL, 350);
        MATERIAL_FUEL_VALUES.put(Items.TOTEM_OF_UNDYING, 400);
        MATERIAL_FUEL_VALUES.put(Items.MUSIC_DISC_PIGSTEP, 200);
        MATERIAL_FUEL_VALUES.put(Items.NETHER_STAR, 200);

        MATERIAL_FUEL_VALUES.put(Items.ENCHANTED_GOLDEN_APPLE, 2000);
        MATERIAL_FUEL_VALUES.put(Items.TRIDENT, 1000);
        MATERIAL_FUEL_VALUES.put(Items.DRAGON_EGG, 2000);
        MATERIAL_FUEL_VALUES.put(Items.BEACON, 750);
    }

    public static int getMaterialFuelValue(Item item) {
        if (MATERIAL_FUEL_VALUES.containsKey(item)) return MATERIAL_FUEL_VALUES.get(item);
        int configVal = Config.getFuelValue(item);
        if (configVal > 0) return configVal;
        return 1;
    }

    public static class FuelBreakdown {
        public int common, uncommon, rare, epic, legendary, total;
        public FuelBreakdown(int c, int u, int r, int e, int l) {
            this.common = c; this.uncommon = u; this.rare = r; this.epic = e; this.legendary = l;
            this.total = c + u + r + e + l;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final List<BlockPos> linkedChests = new ArrayList<>();
    private int cachedBookshelfCount = 0;
    private int cachedMagicFuel = 0;
    private int syncTimer = 0;

    private int clientLinkedChests = 0;
    private int clientBookshelves = 0;
    private int clientMagicFuel = 0;
    private boolean clientHasActivePedestal = false;

    private int clientFuelCommon = 0;
    private int clientFuelUncommon = 0;
    private int clientFuelRare = 0;
    private int clientFuelEpic = 0;
    private int clientFuelLegendary = 0;

    public ArcaneForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_FORGE_BE.get(), pos, blockState);
    }

    public NonNullList<ItemStack> getItems() { return items; }

    public ItemStack getItem(int slot) { return items.get(slot); }

    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

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

    /**
     * Intenta meter un ItemStack en los cofres vinculados a esta mesa.
     * Devuelve lo que NO cupo (vacio si entro todo). Lo usa la Cosecha de Almas
     * para mandar lo recolectado al cofre del jugador.
     */
    public ItemStack insertIntoLinkedChests(ItemStack toInsert) {
        if (level == null || toInsert.isEmpty()) return toInsert;
        ItemStack remaining = toInsert.copy();

        for (BlockPos chestPos : linkedChests) {
            if (remaining.isEmpty()) break;
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (!(be instanceof ChestBlockEntity chest)) continue;

            for (int i = 0; i < chest.getContainerSize() && !remaining.isEmpty(); i++) {
                ItemStack slot = chest.getItem(i);
                if (slot.isEmpty()) {
                    chest.setItem(i, remaining.copy());
                    remaining.setCount(0);
                    chest.setChanged();
                } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    if (space > 0) {
                        int moved = Math.min(space, remaining.getCount());
                        slot.grow(moved);
                        remaining.shrink(moved);
                        chest.setChanged();
                    }
                }
            }
        }
        return remaining;
    }
    public int getLinkedChestCount() { return linkedChests.size(); }

    /**
     * Recalcula el fuel/cofres/pedestal AHORA mismo y se lo manda al jugador
     * que acaba de abrir la mesa, para que no vea datos viejos (0) esperando
     * al siguiente tick de sincronizacion.
     */
    public void forceSyncTo(ServerPlayer player) {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;
        validateLinkedChests();
        cachedBookshelfCount = countNearbyBookshelves();
        cachedMagicFuel = countMagicFuelInLinkedChests();
        boolean hasActivePedestal = ArcanePedestalBlock.hasActivePedestalNearby(level, worldPosition);
        FuelBreakdown breakdown = computeFuelBreakdown();
        S2CSyncPacket pkt = new S2CSyncPacket(
                worldPosition, linkedChests.size(), cachedBookshelfCount, cachedMagicFuel, hasActivePedestal,
                breakdown.common, breakdown.uncommon, breakdown.rare, breakdown.epic, breakdown.legendary);
        PacketDistributor.sendToPlayer(player, pkt);
    }

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
                    BlockState bs = level.getBlockState(center.offset(x, y, z));
                    if (bs.is(Blocks.BOOKSHELF)) count++;
                }
            }
        }
        return Config.MAX_BOOKSHELF_COUNT > 0 ? Math.min(count, Config.MAX_BOOKSHELF_COUNT) : count;
    }

    public int getNearbyBookshelfCount() { return cachedBookshelfCount; }

    public static int calculateProgressiveCost(int currentLevel, int levelsToAdd, int bookshelves, boolean hasPedestal) {
        int totalCost = 0;
        int targetLevel = currentLevel + levelsToAdd;

        for (int lvl = currentLevel; lvl < targetLevel; lvl++) {
            int stepCost = 8;
            if (lvl >= 5 && lvl < 10) stepCost += (lvl * 5);
            else if (lvl >= 10 && lvl < 30) stepCost += (lvl * 25);
            else if (lvl >= 30) stepCost += (lvl * 65);
            totalCost += stepCost;
        }

        float discount = Math.min(0.40f, bookshelves * 0.02f);
        totalCost = Math.round(totalCost * (1.0f - discount));
        if (!hasPedestal) totalCost = Math.round(totalCost * 1.75f);

        return Math.max(1, totalCost);
    }

    public int countMagicFuelInLinkedChests() {
        if (level == null) return 0;
        int total = 0;
        for (BlockPos chestPos : linkedChests) {
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (!stack.isEmpty()) total += getMaterialFuelValue(stack.getItem()) * stack.getCount();
                }
            }
        }
        return total;
    }

    public FuelBreakdown computeFuelBreakdown() {
        if (level == null) return new FuelBreakdown(0, 0, 0, 0, 0);
        int common = 0, uncommon = 0, rare = 0, epic = 0, legendary = 0;

        for (BlockPos chestPos : linkedChests) {
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.isEmpty()) continue;

                    int val = getMaterialFuelValue(stack.getItem());
                    int totalVal = val * stack.getCount();

                    if (val >= 750) legendary += totalVal;
                    else if (val >= 200) epic += totalVal;
                    else if (val >= 50) rare += totalVal;
                    else if (val >= 10) uncommon += totalVal;
                    else common += totalVal;
                }
            }
        }
        return new FuelBreakdown(common, uncommon, rare, epic, legendary);
    }

    public int getTotalMagicFuel() { return cachedMagicFuel; }

    private boolean extractMagicFuel(int requiredPoints) {
        if (level == null) return false;

        List<SlotRef> allSlots = new ArrayList<>();
        for (BlockPos chestPos : linkedChests) {
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (!stack.isEmpty()) {
                        allSlots.add(new SlotRef(chest, i, getMaterialFuelValue(stack.getItem())));
                    }
                }
            }
        }

        allSlots.sort(Comparator.comparingInt(a -> a.fuelPerUnit));

        int remaining = requiredPoints;
        for (SlotRef ref : allSlots) {
            if (remaining <= 0) break;

            ItemStack stack = ref.chest.getItem(ref.slot);
            if (stack.isEmpty()) continue;

            int fuelValue = ref.fuelPerUnit;
            int itemsNeeded = (int) Math.ceil((double) remaining / fuelValue);
            int itemsToTake = Math.min(itemsNeeded, stack.getCount());
            int pointsGained = itemsToTake * fuelValue;

            stack.shrink(itemsToTake);
            if (stack.isEmpty()) ref.chest.setItem(ref.slot, ItemStack.EMPTY);
            remaining -= pointsGained;
            ref.chest.setChanged();
        }

        return remaining <= 0;
    }

    private static class SlotRef {
        final ChestBlockEntity chest;
        final int slot;
        final int fuelPerUnit;
        SlotRef(ChestBlockEntity chest, int slot, int fuelPerUnit) {
            this.chest = chest;
            this.slot = slot;
            this.fuelPerUnit = fuelPerUnit;
        }
    }

    public static float getEnchantmentMultiplier(Identifier id) {
        String path = id.getPath();
        String namespace = id.getNamespace();

        if (namespace.equals(ArcaneForge.MODID)) {
            if (path.equals("apocalyptic_judgment") || path.equals("void_protection") || path.equals("arcane_loyalty")) return 5.0f;
            if (path.equals("corte_del_vacio") || path.equals("filo_insaciable") || path.equals("alma_perdurable")) return 5.0f;
            if (path.equals("coraza_arcana")) return 5.0f;
            if (path.equals("night_vision")) return 4.0f;
            if (path.equals("miners_sight")) return 2.0f;
            if (path.equals("infinite_bucket")) return 1.0f;
            return 3.0f;
        }
        if (path.equals("mending") || path.equals("infinity") || path.equals("silk_touch")) return 2.5f;
        return 1.0f;
    }

    /**
     * Nivel real maximo de CADA encantamiento — mas alla de esto, subir de
     * nivel en la Forja no cambia absolutamente nada (la formula del efecto
     * ya se aplano ahi), asi que directamente no dejamos gastar mas recursos.
     *
     * PASADA "REWARD 255": casi todo encantamiento con una formula continua
     * (daño, radio, probabilidad, duracion...) ahora escala suave hasta cerca
     * de 250, como los "de bandera" originales — invertir hasta el techo real
     * de la Forja (255 con Pedestal) siempre da ALGO mas, aunque sea caro.
     * Solo se quedan bajos los que son genuinamente binarios (el efecto es
     * "activado/no activado", sin ninguna variable continua que escalar —
     * ej. un cofre ya infinito no puede ser "mas infinito") o los que tienen
     * una restriccion tecnica real de vainilla (Lealtad, ver mas abajo).
     */
    public static int getRealMaxLevel(Identifier id) {
        String path = id.getPath();
        String namespace = id.getNamespace();

        if (namespace.equals(ArcaneForge.MODID)) {
            // "De bandera": escalado suave, aprovechan casi todo el rango 1-255.
            switch (path) {
                case "apocalyptic_judgment", "corte_del_vacio", "filo_insaciable", "coraza_arcana",
                     "golpe_partidor", "floracion_fertil", "carga_instantanea" -> {
                    return 250;
                }
                case "eternal_feast" -> {
                    // Ya tenia su propia curva de rareza pensada para llegar hasta 255.
                    return 255;
                }
                default -> { }
            }
            // Genuinamente binarios: el efecto es todo-o-nada y no hay ninguna
            // variable continua que un nivel mas alto pueda seguir mejorando
            // (un cofre/cubo ya infinito no puede ser "mas infinito", etc.).
            switch (path) {
                case "dimensional_bind", "night_vision",
                     "infinite_bucket", "alma_perdurable",
                     "paso_nevado", "almacen_infinito",
                     "paso_acuatico", "zancada_fantasma" -> {
                    return 1;
                }
                default -> { }
            }
            // El resto: formulas propias re-escaladas para seguir dando algo
            // hasta bien entrado el rango alto, en vez de aplanarse en nivel 3.
            return switch (path) {
                case "arcane_magnet" -> 60;
                case "miners_sight" -> 15;
                case "cadena_arcana", "golpe_dimensional", "filo_eterno",
                     "marca_del_cazador", "golpe_sismico", "sangria_espectral", "vampiro",
                     "ataque_veloz" -> 60;
                case "void_wings" -> 4;
                case "arcane_cataclysm", "chain_thunder", "arcane_repulse" -> 250;
                case "ethereal_hook" -> 30;
                case "ethereal_launch" -> 10;
                case "soul_delve" -> 100;
                case "arcane_mending" -> 200;
                case "soul_harvest" -> 150;
                case "ethereal_walk", "arcane_loyalty" -> 1;
                case "void_protection", "arcane_smelting" -> 175;
                case "paso_infernal" -> 60;
                default -> 3; // por si se anade uno nuevo y se nos olvida registrarlo aqui
            };
        }

        // Vanilla, pero empujados via la Forja: sus efectos en nuestras fusiones
        // (EnchantmentFusionHandler, ArcaneBowFusionHandler, PickaxeArcaneHandler,
        // AxeFortuneHandler) tambien estan topados, asi que no tiene sentido
        // dejar subirlos mas alla de eso.
        return switch (path) {
            // Sharpness/Smite/Bane/Fortune usan la formula lineal propia de
            // vainilla (sin techo natural): dejarlas subir hasta 250 SI premia
            // la inversion, en vez de aplanarlas en nivel 30 como antes.
            case "sharpness", "smite", "bane_of_arthropods", "fortune" -> 250;
            // Carga Rapida vinculada a un Golem de Nieve (ver ArcaneSnowGolemHandler):
            // el intervalo entre bolas de nieve extra ya toca su piso (2 ticks) en
            // nivel 30, seguir subiendo no acelera mas el disparo.
            case "quick_charge" -> 30;
            // Mending/Infinity/Silk Touch son binarios de verdad en vainilla:
            // el nivel no cambia su efecto en absoluto (Mending repara igual,
            // Infinity no gasta flechas igual, Silk Touch conserva el bloque
            // igual), asi que subirlos de nivel 1 es gastar recursos por nada.
            case "mending", "infinity", "silk_touch" -> 1;
            // Lealtad empuja al tridente hacia el dueno con velocidad de regimen
            // permanente = nivel bloques/tick (ThrownTrident.tick: accel=0.05*nivel,
            // decay=0.95 => v_estable = accel/0.05 = nivel). A nivel 255 (topado a
            // byte 127 por vanilla) eso son ~127 bloques/tick: el tridente vuela mas
            // alla del jugador en cada tick, nunca entra en rango de recogida, y
            // queda "flotando" en una orbita/rebote que parece congelado. Vainilla
            // nunca prueba mas alla de nivel 3, asi que topamos ahi para que la
            // mecanica de retorno funcione como fue disenada — es la UNICA
            // restriccion de esta lista que es tecnica y no de balance.
            case "loyalty" -> 3;
            default -> Integer.MAX_VALUE; // no tocamos otros encantamientos vanilla
        };
    }

    /** Nivel actual de un encantamiento en el item que hay en la Forja ahora mismo, o 0. */
    public int getCurrentEnchantLevel(Identifier enchantmentId) {
        if (level == null) return 0;
        ItemStack stack = items.getFirst();
        if (stack.isEmpty()) return 0;
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(key);
            if (opt.isEmpty()) return 0;
            ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get());
        } catch (Exception e) {
            return 0;
        }
    }

    public int tryEnchant(Identifier enchantmentId, int targetLevel, ServerPlayer player) {
        if (level == null || level.isClientSide()) return -1;

        ItemStack itemToEnchant = items.getFirst();
        if (itemToEnchant.isEmpty()) return -1;

        cachedBookshelfCount = countNearbyBookshelves();
        cachedMagicFuel = countMagicFuelInLinkedChests();

        ItemEnchantments currentEnchants = itemToEnchant.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int currentLevel = 0;
        Holder<Enchantment> enchHolder = null;

        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);
            Optional<Holder.Reference<Enchantment>> optHolder = registry.get(key);
            if (optHolder.isPresent()) {
                enchHolder = optHolder.get();
                currentLevel = currentEnchants.getLevel(enchHolder);
            }
        } catch (Exception ignored) {
        }

        if (enchHolder == null) return -1;

        // FUSION: aplicar un encantamiento naturalmente incompatible con otro
        // que el item YA tiene (Toque de Seda+Fortuna, Filo+Smite/Perdicion,
        // dos Protecciones distintas, etc. — cualquier exclusive_set real de
        // vanilla, via Enchantment.areCompatible) exige tener un Pedestal o un
        // Bloque de Poder Arcano cerca. Sin bloque, la Forja se comporta como
        // una mesa de encantar normal: no deja combinar exclusivos entre si.
        List<Holder<Enchantment>> otherEnchants = new ArrayList<>();
        for (var entry : currentEnchants.entrySet()) {
            if (!entry.getKey().equals(enchHolder)) otherEnchants.add(entry.getKey());
        }
        if (!EnchantmentHelper.isEnchantmentCompatible(otherEnchants, enchHolder)
                && !hasFusionBlockNearby(level, worldPosition)) {
            return -2;
        }

        boolean hasPedestal = ArcanePedestalBlock.hasActivePedestalNearby(level, worldPosition);
        // REBALANCE: sin pedestal el tope es 15 (estilo vanilla). Para superar 15
        // hace falta un Pedestal activo, que sube el maximo hasta 255. El coste de
        // recursos y XP sube con el nivel, asi que llegar a 255 es caro a proposito.
        int maxAllowedLevel = hasPedestal ? 255 : 15;

        int finalLevel = currentLevel + targetLevel;
        if (finalLevel > maxAllowedLevel) {
            targetLevel = maxAllowedLevel - currentLevel;
            finalLevel = maxAllowedLevel;
        }
        if (targetLevel <= 0) return -1;

        int baseCost = calculateProgressiveCost(currentLevel, targetLevel, cachedBookshelfCount, hasPedestal);
        int totalCost = Math.max(1, Math.round(baseCost * getEnchantmentMultiplier(enchantmentId)));

        // Rama MATERIAL de los bloques de descuento del aldeano: abarata el magic fuel.
        // (El coste en EXP ya existe aparte, en C2SEnchantPacket.handle() — la rama XP
        // de los bloques de descuento se aplica ahi, no aqui, para no cobrar EXP dos veces.)
        float materialDiscount = ArcaneDiscountBlock.getBestDiscount(level, worldPosition, ArcaneDiscountBlock.Branch.MATERIAL);
        if (materialDiscount > 0f) totalCost = Math.max(1, Math.round(totalCost * (1.0f - materialDiscount)));

        if (player.isCreative()) totalCost = 0;
        if (cachedMagicFuel < totalCost) return -1;
        if (totalCost > 0 && !extractMagicFuel(totalCost)) return -1;

        forceApplyEnchantment(itemToEnchant, enchHolder, finalLevel, hasPedestal);
        setChanged();
        cachedMagicFuel = countMagicFuelInLinkedChests();
        return finalLevel;
    }

    /**
     * Baja el nivel de un encantamiento que el item ya tiene. No toca el magic
     * fuel (ya se pago al subirlo la primera vez); el reembolso de EXP se
     * calcula y se entrega en C2SEnchantPacket, aqui solo se reescribe el
     * componente de encantamientos del item. Si finalLevel llega a 0,
     * ItemEnchantments.Mutable.set() ya lo quita del item por su cuenta.
     */
    public int tryLowerEnchant(Identifier enchantmentId, int levelsToRemove, ServerPlayer player) {
        if (level == null || level.isClientSide()) return -1;

        ItemStack itemToEnchant = items.getFirst();
        if (itemToEnchant.isEmpty()) return -1;

        ItemEnchantments currentEnchants = itemToEnchant.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        Holder<Enchantment> enchHolder = null;
        int currentLevel = 0;

        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);
            Optional<Holder.Reference<Enchantment>> optHolder = registry.get(key);
            if (optHolder.isPresent()) {
                enchHolder = optHolder.get();
                currentLevel = currentEnchants.getLevel(enchHolder);
            }
        } catch (Exception ignored) {
        }

        if (enchHolder == null || currentLevel <= 0) return -1;
        levelsToRemove = Math.min(levelsToRemove, currentLevel);
        if (levelsToRemove <= 0) return -1;

        int finalLevel = currentLevel - levelsToRemove;
        boolean hasPedestal = ArcanePedestalBlock.hasActivePedestalNearby(level, worldPosition);

        forceApplyEnchantment(itemToEnchant, enchHolder, finalLevel, hasPedestal);
        setChanged();
        return finalLevel;
    }

    /** Mismo radio/bloques que EnchantmentFusionHandler.hasFusionBlockNearby. */
    private static boolean hasFusionBlockNearby(Level level, BlockPos centerPos) {
        int radius = 5;
        for (BlockPos p : BlockPos.betweenClosed(centerPos.offset(-radius, -radius, -radius), centerPos.offset(radius, radius, radius))) {
            BlockState checkState = level.getBlockState(p);
            if (checkState.is(ModBlocks.ARCANE_POWER_BLOCK.get()) || checkState.is(ModBlocks.ARCANE_PEDESTAL.get())) {
                return true;
            }
        }
        return false;
    }

    private void forceApplyEnchantment(ItemStack stack, Holder<Enchantment> holder, int level, boolean hasPedestal) {
        ItemStack copy = stack.copy();
        ItemEnchantments currentEnchants = copy.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchants);

        mutable.set(holder, level);
        copy.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        if (copy.is(Items.TOTEM_OF_UNDYING)) {
            net.minecraft.world.item.component.CustomData customData = copy.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            tag.putInt("TotemCharges", 3);
            copy.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }

        if (hasPedestal) {
            net.minecraft.world.item.component.CustomData customData = copy.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            tag.putBoolean("ArcaneAwakened", true);
            copy.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
        this.items.set(0, copy);
    }

    public void setClientSyncData(int chests, int bookshelves, int magicFuel, boolean hasActivePedestal) {
        this.clientLinkedChests = chests;
        this.clientBookshelves = bookshelves;
        this.clientMagicFuel = magicFuel;
        this.clientHasActivePedestal = hasActivePedestal;
    }

    public void setClientFuelBreakdown(int common, int uncommon, int rare, int epic, int legendary) {
        this.clientFuelCommon = common;
        this.clientFuelUncommon = uncommon;
        this.clientFuelRare = rare;
        this.clientFuelEpic = epic;
        this.clientFuelLegendary = legendary;
    }

    public int getClientLinkedChests() { return clientLinkedChests; }
    public int getClientBookshelves() { return clientBookshelves; }
    public int getClientMagicFuel() { return clientMagicFuel; }
    public boolean hasActivePedestalNearby() { return clientHasActivePedestal; }

    public int getClientFuelCommon() { return clientFuelCommon; }
    public int getClientFuelUncommon() { return clientFuelUncommon; }
    public int getClientFuelRare() { return clientFuelRare; }
    public int getClientFuelEpic() { return clientFuelEpic; }
    public int getClientFuelLegendary() { return clientFuelLegendary; }

    @SuppressWarnings("unused")
    public static void tick(Level level, BlockPos pos, BlockState state, ArcaneForgeBlockEntity be) {
        if (!level.isClientSide()) {
            be.syncTimer++;
            if (be.syncTimer >= Config.SYNC_INTERVAL_TICKS) {
                be.syncTimer = 0;
                be.validateLinkedChests();
                be.cachedBookshelfCount = be.countNearbyBookshelves();
                be.cachedMagicFuel = be.countMagicFuelInLinkedChests();

                boolean hasActivePedestal = ArcanePedestalBlock.hasActivePedestalNearby(level, pos);

                if (level instanceof ServerLevel serverLevel) {
                    FuelBreakdown breakdown = be.computeFuelBreakdown();
                    S2CSyncPacket syncPacket = new S2CSyncPacket(
                            pos, be.linkedChests.size(), be.cachedBookshelfCount, be.cachedMagicFuel, hasActivePedestal,
                            breakdown.common, breakdown.uncommon, breakdown.rare, breakdown.epic, breakdown.legendary);

                    for (ServerPlayer player : serverLevel.players()) {
                        if (player.containerMenu instanceof ArcaneForgeMenu forgeMenu && forgeMenu.getBlockEntity() == be) {
                            PacketDistributor.sendToPlayer(player, syncPacket);
                        }
                    }
                }
            }
        } else {
            if (be.clientHasActivePedestal) {
                RandomSource random = level.getRandom();
                long time = level.getGameTime();

                if (time % 3 == 0) {
                    for (BlockPos chestPos : be.getLinkedChests()) {
                        double dx = (pos.getX() + 0.5) - (chestPos.getX() + 0.5);
                        double dy = (pos.getY() + 1.2) - (chestPos.getY() + 0.8);
                        double dz = (pos.getZ() + 0.5) - (chestPos.getZ() + 0.5);

                        level.addParticle(new DustParticleOptions(0x8A2BE2, 0.9f),
                                chestPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                                chestPos.getY() + 0.8 + random.nextDouble() * 0.5,
                                chestPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                                dx * 0.06, dy * 0.06, dz * 0.06);
                    }
                }

                double angle = time * 0.15;
                double radius = 0.8 + Math.sin(time * 0.05) * 0.2;
                double py = pos.getY() + 0.5 + (Math.sin(time * 0.1) * 0.5);

                level.addParticle(new DustParticleOptions(0xFF00FF, 1.2f),
                        pos.getX() + 0.5 + Math.cos(angle) * radius, py,
                        pos.getZ() + 0.5 + Math.sin(angle) * radius, 0, 0.02, 0);
                level.addParticle(new DustParticleOptions(0x00FFFF, 1.2f),
                        pos.getX() + 0.5 + Math.cos(angle + Math.PI) * radius, py,
                        pos.getZ() + 0.5 + Math.sin(angle + Math.PI) * radius, 0, 0.02, 0);
            }
        }
    }

    public void validateLinkedChests() {
        if (level == null) return;
        boolean changed = linkedChests.removeIf(chestPos -> {
            if (!level.isLoaded(chestPos)) return false;
            BlockEntity be = level.getBlockEntity(chestPos);
            return !(be instanceof ChestBlockEntity);
        });
        if (changed) setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!items.getFirst().isEmpty()) output.store("slot_0", ItemStack.CODEC, items.getFirst());
        output.putInt("chest_count", linkedChests.size());
        for (int i = 0; i < linkedChests.size(); i++) {
            BlockPos cp = linkedChests.get(i);
            output.putInt("chest_" + i + "_x", cp.getX());
            output.putInt("chest_" + i + "_y", cp.getY());
            output.putInt("chest_" + i + "_z", cp.getZ());
        }
        output.putInt("bookshelf_count", cachedBookshelfCount);
        output.putInt("magic_fuel", cachedMagicFuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.set(0, input.read("slot_0", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        linkedChests.clear();
        int chestCount = input.getIntOr("chest_count", 0);
        for (int i = 0; i < chestCount; i++) {
            int cx = input.getIntOr("chest_" + i + "_x", 0);
            int cy = input.getIntOr("chest_" + i + "_y", 0);
            int cz = input.getIntOr("chest_" + i + "_z", 0);
            linkedChests.add(new BlockPos(cx, cy, cz));
        }
        cachedBookshelfCount = input.getIntOr("bookshelf_count", 0);
        cachedMagicFuel = input.getIntOr("magic_fuel", 0);
    }

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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arcaneforge.arcane_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcaneForgeMenu(containerId, playerInventory, this);
    }
}