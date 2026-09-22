package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.Andres.arcaneforge.registry.ModItems;
import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ver ArcaneGolemGeneratorBlock para la explicacion general. Aqui vive el
 * ciclo de produccion: cada Config.GOLEM_GENERATOR_INTERVAL_TICKS, si hay
 * un Nucleo insertado y los cofres vinculados tienen el doble de la receta
 * vainilla de un golem de hierro (Config.GOLEM_GENERATOR_IRON_BLOCKS +
 * Config.GOLEM_GENERATOR_PUMPKINS), retira esos materiales y spawnea un
 * IronGolem nuevo con los mismos encantamientos/bonos que uno vinculado a
 * mano con el Cetro — sin tocar la experiencia del jugador para nada mas
 * que el encantado inicial del Nucleo en la Forja (eso ya pasa por el
 * camino normal de la Forja, ver ArcaneForgeBlockEntity.tryEnchant +
 * C2SEnchantPacket).
 */
public class ArcaneGolemGeneratorBlockEntity extends BlockEntity {

    private ItemStack core = ItemStack.EMPTY;
    private UUID ownerUUID = null;
    private final List<BlockPos> linkedChests = new ArrayList<>();
    private int prodTimer = 0;

    public ArcaneGolemGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GOLEM_GENERATOR_BE.get(), pos, blockState);
    }

    public boolean hasCore() { return !core.isEmpty(); }

    public boolean insertCore(ItemStack stack, Player player) {
        if (hasCore()) return false;
        if (!stack.is(ModItems.GENERATOR_CORE.get())) return false;
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) return false;

        core = stack.copyWithCount(1);
        ownerUUID = player.getUUID();
        setChanged();
        return true;
    }

    public ItemStack ejectCore() {
        ItemStack out = core;
        core = ItemStack.EMPTY;
        ownerUUID = null;
        setChanged();
        return out;
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

    public int getLinkedChestCount() { return linkedChests.size(); }

    private void validateLinkedChests() {
        if (level == null) return;
        boolean changed = linkedChests.removeIf(chestPos -> {
            if (!level.isLoaded(chestPos)) return false;
            BlockEntity be = level.getBlockEntity(chestPos);
            return !(be instanceof ChestBlockEntity);
        });
        if (changed) setChanged();
    }

    private int countItem(Item item) {
        if (level == null) return 0;
        int total = 0;
        for (BlockPos chestPos : linkedChests) {
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.is(item)) total += stack.getCount();
                }
            }
        }
        return total;
    }

    /** Asume que ya se confirmo que hay suficiente (via countItem) antes de llamar esto. */
    private void removeItem(Item item, int amount) {
        if (level == null) return;
        int remaining = amount;
        for (BlockPos chestPos : linkedChests) {
            if (remaining <= 0) break;
            if (!level.isLoaded(chestPos)) continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.is(item)) {
                        int taken = Math.min(remaining, stack.getCount());
                        stack.shrink(taken);
                        remaining -= taken;
                        chest.setChanged();
                    }
                }
            }
        }
    }

    /** Busca una posicion libre (2 bloques de aire con piso solido debajo) cerca del generador. */
    private BlockPos findSpawnPos(Level level, BlockPos origin) {
        BlockPos[] candidates = {
                origin.above(), origin.north().above(), origin.south().above(),
                origin.east().above(), origin.west().above()
        };
        for (BlockPos candidate : candidates) {
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                return candidate;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void tick(Level level, BlockPos pos, BlockState state, ArcaneGolemGeneratorBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        be.prodTimer++;
        if (be.prodTimer < Config.GOLEM_GENERATOR_INTERVAL_TICKS) return;
        be.prodTimer = 0;

        if (!be.hasCore()) return;

        be.validateLinkedChests();

        AABB nearby = new AABB(pos).inflate(Config.GOLEM_GENERATOR_NEARBY_RADIUS);
        if (serverLevel.getEntitiesOfClass(AbstractGolem.class, nearby).size() >= Config.GOLEM_GENERATOR_MAX_NEARBY) {
            return;
        }

        boolean hasIronMaterials = be.countItem(Items.IRON_BLOCK) >= Config.GOLEM_GENERATOR_IRON_BLOCKS
                && be.countItem(Items.CARVED_PUMPKIN) >= Config.GOLEM_GENERATOR_PUMPKINS;
        boolean hasSnowMaterials = be.countItem(Items.SNOW_BLOCK) >= Config.GOLEM_GENERATOR_SNOW_BLOCKS
                && be.countItem(Items.CARVED_PUMPKIN) >= Config.GOLEM_GENERATOR_PUMPKINS;
        if (!hasIronMaterials && !hasSnowMaterials) return;

        // Si el cofre tiene AMBOS materiales a la vez, elegir al azar en vez
        // de siempre priorizar hierro — de lo contrario, mientras haya hierro
        // de sobra, nunca sale un golem de nieve aunque tambien haya nieve.
        boolean spawnIron = hasIronMaterials && hasSnowMaterials
                ? serverLevel.getRandom().nextBoolean()
                : hasIronMaterials;

        BlockPos spawnPos = be.findSpawnPos(level, pos);
        if (spawnPos == null) return;

        if (spawnIron) {
            be.removeItem(Items.IRON_BLOCK, Config.GOLEM_GENERATOR_IRON_BLOCKS);
            be.removeItem(Items.CARVED_PUMPKIN, Config.GOLEM_GENERATOR_PUMPKINS);

            IronGolem golem = EntityType.IRON_GOLEM.spawn(serverLevel, spawnPos, EntitySpawnReason.SPAWNER);
            if (golem == null) return;

            ArcaneGolemUtil.bindWandEnchantsToGolem(golem, be.core);
            if (be.ownerUUID != null) ArcaneGolemUtil.setOwner(golem, be.ownerUUID);
            ArcaneGolemUtil.applyGolemBonuses(golem);
            golem.setCustomName(Component.literal("Golem Arcano"));
            golem.setCustomNameVisible(true);
        } else {
            be.removeItem(Items.SNOW_BLOCK, Config.GOLEM_GENERATOR_SNOW_BLOCKS);
            be.removeItem(Items.CARVED_PUMPKIN, Config.GOLEM_GENERATOR_PUMPKINS);

            SnowGolem golem = EntityType.SNOW_GOLEM.spawn(serverLevel, spawnPos, EntitySpawnReason.SPAWNER);
            if (golem == null) return;

            ArcaneGolemUtil.bindWandEnchantsToGolem(golem, be.core);
            if (be.ownerUUID != null) ArcaneGolemUtil.setOwner(golem, be.ownerUUID);
            ArcaneGolemUtil.applyGolemBonuses(golem);
            golem.setCustomName(Component.literal("Golem de Nieve Arcano"));
            golem.setCustomNameVisible(true);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!core.isEmpty()) output.store("core", ItemStack.CODEC, core);
        if (ownerUUID != null) output.putString("owner", ownerUUID.toString());
        output.putInt("chest_count", linkedChests.size());
        for (int i = 0; i < linkedChests.size(); i++) {
            BlockPos cp = linkedChests.get(i);
            output.putInt("chest_" + i + "_x", cp.getX());
            output.putInt("chest_" + i + "_y", cp.getY());
            output.putInt("chest_" + i + "_z", cp.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        core = input.read("core", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        String ownerRaw = input.getStringOr("owner", "");
        try {
            ownerUUID = ownerRaw.isEmpty() ? null : UUID.fromString(ownerRaw);
        } catch (IllegalArgumentException e) {
            ownerUUID = null;
        }
        linkedChests.clear();
        int chestCount = input.getIntOr("chest_count", 0);
        for (int i = 0; i < chestCount; i++) {
            int cx = input.getIntOr("chest_" + i + "_x", 0);
            int cy = input.getIntOr("chest_" + i + "_y", 0);
            int cz = input.getIntOr("chest_" + i + "_z", 0);
            linkedChests.add(new BlockPos(cx, cy, cz));
        }
    }
}
