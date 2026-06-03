package com.Andres.arcaneforge.item;

import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Binding Wand — Vincula cofres a la Arcane Forge.
 *
 * VERSIÓN CORREGIDA para NeoForge 26.1.2:
 * - Usa DataComponents.CUSTOM_DATA en lugar de getTag()/getOrCreateTag() (eliminados)
 * - Almacena posiciones como ints individuales (chest_count + chest_N_x/y/z)
 *   en lugar de ListTag (evita problemas con API de CompoundTag modificada)
 * - Usa Config.MAX_LINKED_CHESTS (256) para el límite
 */
public class BindingWand extends Item {

    public BindingWand(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack wand = context.getItemInHand();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);

        // ── Clic derecho en un COFRE → alternar vínculo ──
        if (be instanceof ChestBlockEntity) {
            List<BlockPos> stored = getStoredChests(wand);

            // Verificar si ya está almacenado → desvincular
            boolean wasStored = stored.removeIf(p -> p.equals(pos));
            if (wasStored) {
                saveStoredChests(wand, stored);
                player.sendSystemMessage(
                        Component.literal("§c✖ Cofre desvinculado: " + formatPos(pos)
                                + " (" + stored.size() + "/" + Config.MAX_LINKED_CHESTS + ")"));
                return InteractionResult.CONSUME;
            }

            // Verificar capacidad
            if (stored.size() >= Config.MAX_LINKED_CHESTS) {
                player.sendSystemMessage(
                        Component.literal("§cMax " + Config.MAX_LINKED_CHESTS
                                + " cofres! Shift+clic derecho para limpiar."));
                return InteractionResult.CONSUME;
            }

            // Añadir nuevo cofre
            stored.add(pos.immutable());
            saveStoredChests(wand, stored);
            player.sendSystemMessage(
                    Component.literal("§a✔ Cofre vinculado: " + formatPos(pos)
                            + " (" + stored.size() + "/" + Config.MAX_LINKED_CHESTS + ")"));

            wand.hurtAndBreak(1, player, context.getHand());
            return InteractionResult.CONSUME;
        }

        // ── Clic derecho en ARCANE FORGE → transferir todos los cofres ──
        if (be instanceof ArcaneForgeBlockEntity forge) {
            // SHIFT + clic derecho en la mesa → marcarla como "mi mesa de cosecha".
            // Asi la Cosecha de Almas sabe a que cofres mandar lo recolectado.
            if (player.isShiftKeyDown()) {
                CompoundTag pdata = player.getPersistentData();
                pdata.putInt("arcaneforge_harvest_x", pos.getX());
                pdata.putInt("arcaneforge_harvest_y", pos.getY());
                pdata.putInt("arcaneforge_harvest_z", pos.getZ());
                player.sendSystemMessage(Component.literal(
                        "§b✔ Mesa de cosecha marcada: " + formatPos(pos)
                        + ". La Cosecha de Almas enviara aqui lo recolectado."));
                return InteractionResult.CONSUME;
            }

            List<BlockPos> stored = getStoredChests(wand);
            if (stored.isEmpty()) {
                player.sendSystemMessage(
                        Component.literal("§eSin cofres almacenados! Clic derecho en cofres primero."));
                return InteractionResult.CONSUME;
            }

            int linked = 0;
            for (BlockPos chestPos : stored) {
                if (forge.addLinkedChest(chestPos)) {
                    linked++;
                }
            }

            clearStoredChests(wand);

            if (linked > 0) {
                player.sendSystemMessage(
                        Component.literal("§a✔ " + linked + " cofre(s) vinculado(s) a Arcane Forge! "
                                + "(" + forge.getLinkedChestCount() + "/" + Config.MAX_LINKED_CHESTS + ")"));
                wand.hurtAndBreak(2, player, context.getHand());
            } else {
                player.sendSystemMessage(
                        Component.literal("§eTodos los cofres ya estaban vinculados o forja llena."));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    // ════════════════════════════════════
    // Almacenamiento de cofres via DataComponents.CUSTOM_DATA
    //
    // En 26.1.2 ya NO existe ItemStack.getTag() ni getOrCreateTag().
    // Se usa DataComponents.CUSTOM_DATA + CustomData como wrapper.
    //
    // Formato interno: CompoundTag con:
    //   "chest_count" → int
    //   "chest_0_x", "chest_0_y", "chest_0_z" → int
    //   "chest_1_x", ... etc.
    // ════════════════════════════════════
    private static List<BlockPos> getStoredChests(ItemStack wand) {
        List<BlockPos> chests = new ArrayList<>();

        CustomData customData = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        // CompoundTag.getInt() devuelve Optional<Integer> en 26.1
        int count = tag.getInt("chest_count").orElse(0);
        for (int i = 0; i < count; i++) {
            int x = tag.getInt("chest_" + i + "_x").orElse(0);
            int y = tag.getInt("chest_" + i + "_y").orElse(0);
            int z = tag.getInt("chest_" + i + "_z").orElse(0);
            chests.add(new BlockPos(x, y, z));
        }
        return chests;
    }

    private static void saveStoredChests(ItemStack wand, List<BlockPos> chests) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("chest_count", chests.size());
        for (int i = 0; i < chests.size(); i++) {
            BlockPos p = chests.get(i);
            tag.putInt("chest_" + i + "_x", p.getX());
            tag.putInt("chest_" + i + "_y", p.getY());
            tag.putInt("chest_" + i + "_z", p.getZ());
        }
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearStoredChests(ItemStack wand) {
        wand.remove(DataComponents.CUSTOM_DATA);
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }
}
