package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.item.BagContainer;
import com.Andres.arcaneforge.item.BagLogic;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
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
 *   • items sueltos cercanos en el suelo (nivel 3)
 * se transfieren automáticamente a la bolsa en lugar de caer al suelo.
 *
 * Nivel 1 → solo bloques
 * Nivel 2 → bloques + mobs
 * Nivel 3 → bloques + mobs + items cercanos al suelo (radio 8 bloques)
 *
 * Si el jugador tiene la GUI de la bolsa abierta, las inserciones se hacen
 * directamente sobre el Container en vivo que respalda esa pantalla, para que
 * se vean al instante y no se pierdan al cerrar la ventana.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneMagnetHandler {

    private static final ResourceKey<Enchantment> MAGNET_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_magnet"));

    // ── Drops de bloque → a la bolsa ──────────────────────────────────────────

    /**
     * LOWEST: debe correr DESPUES de PickaxeArcaneHandler (Fundicion Arcana,
     * Fortune+Silk Touch), AxeFortuneHandler, SoulHarvestHoeHandler, etc. Esos
     * handlers transforman/multiplican los drops (ej. hierro en bruto -> lingote
     * de hierro). Si el iman corriera primero, se llevaria el mineral CRUDO a la
     * bolsa antes de que esos encantamientos lo procesen.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        try {
            if (!(event.getBreaker() instanceof ServerPlayer sp)) return;

            int magnetLevel = getMagnetLevel(sp);
            if (magnetLevel < 1) return;

            ItemStack bag = findBag(sp);
            if (bag.isEmpty()) return;
            if (!BagLogic.isMagnetEnabled(bag)) return;

            List<ItemEntity> toRemove = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                if (insertIntoBag(sp, bag, ie.getItem())) {
                    toRemove.add(ie);
                }
            }
            event.getDrops().removeAll(toRemove);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneMagnet block drop error: {}", e.getMessage());
        }
    }

    // ── Drops de mob → a la bolsa ─────────────────────────────────────────────

    /** LOWEST por la misma razon que onBlockDrops: capturar el resultado final. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobDrops(LivingDropsEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;

            int magnetLevel = getMagnetLevel(sp);
            if (magnetLevel < 2) return;

            ItemStack bag = findBag(sp);
            if (bag.isEmpty()) return;
            if (!BagLogic.isMagnetEnabled(bag)) return;

            List<ItemEntity> toRemove = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                if (insertIntoBag(sp, bag, ie.getItem())) {
                    toRemove.add(ie);
                }
            }
            event.getDrops().removeAll(toRemove);

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
            if (!BagLogic.isMagnetEnabled(bag)) return;

            AABB area = sp.getBoundingBox().inflate(VACUUM_RADIUS);
            List<ItemEntity> nearby = sp.level().getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
            if (nearby.isEmpty()) return;

            for (ItemEntity ie : nearby) {
                ItemStack stack = ie.getItem();
                boolean fullyAbsorbed = insertIntoBag(sp, bag, stack);
                if (fullyAbsorbed) {
                    ie.discard();
                } else if (stack.getCount() != ie.getItem().getCount()) {
                    ie.setItem(stack);
                }
            }

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneMagnet ground vacuum error: {}", e.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Inserta el stack en la bolsa. Si el jugador tiene la GUI de ESTA bolsa
     * abierta ahora mismo, escribe directamente en el Container en vivo del
     * menu (para que se vea al instante y no se sobreescriba al cerrar).
     * Si no, abre/guarda un BagContainer aparte sobre el NBT de la bolsa.
     * Devuelve true si el stack quedo COMPLETAMENTE absorbido.
     */
    private static boolean insertIntoBag(ServerPlayer sp, ItemStack bag, ItemStack stack) {
        if (sp.containerMenu instanceof DimensionalBagMenu bagMenu && bagMenu.isForBag(bag)) {
            Container live = bagMenu.getStorage();
            boolean absorbed = tryInsert(live, stack);
            live.setChanged(); // guarda en NBT
            // El broadcast ambiental de Minecraft NO detecta este cambio de fondo
            // (solo lo hace al procesar un clic del jugador). Empujar el paquete
            // a mano para que el numero se vea actualizado sin tener que agarrarlo.
            bagMenu.syncStorageToClient(sp);
            return absorbed;
        }
        BagContainer container = new BagContainer(bag, 0, sp.level().registryAccess());
        boolean absorbed = tryInsert(container, stack);
        container.saveNow();
        return absorbed;
    }

    /**
     * Intenta insertar el stack en el primer slot disponible del Container.
     * Respeta el límite de BagContainer.MAX_STACK por slot.
     * Devuelve true si el stack quedó COMPLETAMENTE absorbido.
     */
    private static boolean tryInsert(Container container, ItemStack stack) {
        if (stack.isEmpty()) return true;

        // Primero intentar apilar sobre items existentes del mismo tipo
        for (int i = 0; i < BagContainer.PAGE_SIZE; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                long space = (long) BagContainer.MAX_STACK - slot.getCount();
                if (space > 0) {
                    int take = (int) Math.min(space, stack.getCount());
                    // IMPORTANTE: crear un ItemStack NUEVO en vez de mutar "slot" (la
                    // referencia que ya vive dentro del container) en el sitio. Si se
                    // muta en el sitio, el mecanismo de sincronizacion de Minecraft
                    // (que compara la referencia "antes" contra la "ahora" para decidir
                    // si avisar al cliente) ve el MISMO objeto y nunca detecta el
                    // cambio: el numero se queda congelado en el ultimo valor que si
                    // se sincronizo, aunque el dato real en el servidor haya crecido.
                    ItemStack grown = slot.copy();
                    grown.grow(take);
                    stack.shrink(take);
                    container.setItem(i, grown);
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
