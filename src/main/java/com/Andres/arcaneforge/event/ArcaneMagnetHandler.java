package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.BagContainer;
import com.Andres.arcaneforge.item.BagLogic;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IMÁN ARCANO — si el jugador tiene en el inventario una bolsa (bundle) con
 * el encantamiento "arcane_magnet", todos los drops de:
 *   • bloques minados con pico/hacha/herramienta
 *   • mobs killed (incluidos drops de cualquier fuente de daño del jugador)
 * se transfieren automáticamente a la bolsa en lugar de caer al suelo.
 *
 * Nivel 1 → solo bloques
 * Nivel 2 → bloques + mobs
 * Nivel 3 → bloques + mobs + items cercanos al suelo (radio 8 bloques)
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneMagnetHandler {

    private static final ResourceKey<Enchantment> MAGNET_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_magnet"));

    // ── Drops de bloque → a la bolsa ──────────────────────────────────────────

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        try {
            if (!(event.getBreaker() instanceof ServerPlayer sp)) return;

            int magnetLevel = getMagnetLevel(sp);
            if (magnetLevel < 1) return;

            ItemStack bag = findBag(sp);
            if (bag.isEmpty()) return;

            BagContainer container = new BagContainer(bag, 0,
                    sp.level().registryAccess());

            List<ItemEntity> toRemove = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                if (tryInsert(container, ie.getItem())) {
                    toRemove.add(ie);
                }
            }
            event.getDrops().removeAll(toRemove);
            container.saveNow();

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneMagnet block drop error: {}", e.getMessage());
        }
    }

    // ── Drops de mob → a la bolsa ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;

            int magnetLevel = getMagnetLevel(sp);
            if (magnetLevel < 2) return;

            ItemStack bag = findBag(sp);
            if (bag.isEmpty()) return;

            BagContainer container = new BagContainer(bag, 0,
                    sp.level().registryAccess());

            List<ItemEntity> toRemove = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                if (tryInsert(container, ie.getItem())) {
                    toRemove.add(ie);
                }
            }
            event.getDrops().removeAll(toRemove);
            container.saveNow();

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneMagnet mob drop error: {}", e.getMessage());
        }
    }

    // ── Nivel 3: recoge items sueltos del suelo cercano ────────────────────────

    private static final double VACUUM_RADIUS = 8.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            if (sp.tickCount % 10 != 0) return; // cada 0.5s, evita escanear cada tick

            int magnetLevel = getMagnetLevel(sp);
            if (magnetLevel < 3) return;

            ItemStack bag = findBag(sp);
            if (bag.isEmpty()) return;

            AABB area = sp.getBoundingBox().inflate(VACUUM_RADIUS);
            List<ItemEntity> nearby = sp.level().getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
            if (nearby.isEmpty()) return;

            BagContainer container = new BagContainer(bag, 0, sp.level().registryAccess());
            boolean changed = false;

            for (ItemEntity ie : nearby) {
                ItemStack stack = ie.getItem();
                boolean fullyAbsorbed = tryInsert(container, stack);
                if (fullyAbsorbed) {
                    ie.discard();
                    changed = true;
                } else if (stack.getCount() != ie.getItem().getCount()) {
                    ie.setItem(stack);
                    changed = true;
                }
            }
            if (changed) container.saveNow();

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneMagnet ground vacuum error: {}", e.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Intenta insertar el stack en el primer slot disponible del BagContainer.
     * Respeta el límite de BagContainer.MAX_STACK por slot.
     * Devuelve true si el stack quedó COMPLETAMENTE absorbido.
     */
    private static boolean tryInsert(BagContainer container, ItemStack stack) {
        if (stack.isEmpty()) return true;

        // Primero intentar apilar sobre items existentes del mismo tipo
        for (int i = 0; i < BagContainer.PAGE_SIZE; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                long space = (long) BagContainer.MAX_STACK - slot.getCount();
                if (space > 0) {
                    int take = (int) Math.min(space, stack.getCount());
                    slot.grow(take);
                    stack.shrink(take);
                    container.setItem(i, slot);
                    if (stack.isEmpty()) return true;
                }
            }
        }
        // Luego buscar slots vacíos
        for (int i = 0; i < BagContainer.PAGE_SIZE; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
                return true;
            }
        }
        return false; // bolsa llena
    }

    /** Devuelve el nivel de arcane_magnet máximo en el inventario del jugador. */
    private static int getMagnetLevel(ServerPlayer player) {
        try {
            var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> opt = reg.get(MAGNET_KEY);
            if (opt.isEmpty()) return 0;
            int max = 0;
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) continue;
                ItemEnchantments enc = s.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                int lvl = enc.getLevel(opt.get());
                if (lvl > max) max = lvl;
            }
            return max;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Encuentra la primera bolsa (bundle con dimensional_bind > 0) en el inventario.
     * El imán opera sobre esa bolsa.
     */
    private static ItemStack findBag(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(Items.BUNDLE)) continue;
            int lvl = BagLogic.getBindLevel(player, s);
            if (lvl > 0) return s;
        }
        return ItemStack.EMPTY;
    }
}
