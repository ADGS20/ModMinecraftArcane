package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LEALTAD ARCANA — el item que lleve este encantamiento NUNCA se cae al
 * suelo cuando el jugador muere. En vez de dejarlo entre los drops, se
 * aparta en memoria (por UUID del jugador) y se le devuelve intacto justo
 * al reaparecer, como si esa pieza en concreto tuviera su propio
 * "keepInventory" personal.
 *
 * Corre en prioridad NORMAL (por delante del Iman Arcano, que escucha el
 * mismo LivingDropsEvent en LOWEST) para poder sacar el item de la lista de
 * drops ANTES de que cualquier otro handler llegue a verlo.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneLoyaltyHandler {

    private static final ResourceKey<Enchantment> LOYALTY_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_loyalty"));

    /** Items rescatados de la muerte, en espera de que el jugador reaparezca. */
    private static final Map<UUID, List<ItemStack>> SAVED = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;

            var reg = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = reg.get(LOYALTY_KEY);
            if (opt.isEmpty()) return;
            Holder<Enchantment> loyalty = opt.get();

            List<ItemEntity> toRemove = new ArrayList<>();
            List<ItemStack> rescued = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                ItemStack stack = ie.getItem();
                ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                if (ench.getLevel(loyalty) > 0) {
                    rescued.add(stack.copy());
                    toRemove.add(ie);
                }
            }

            if (!rescued.isEmpty()) {
                event.getDrops().removeAll(toRemove);
                SAVED.computeIfAbsent(sp.getUUID(), k -> new ArrayList<>()).addAll(rescued);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneLoyalty drop error: {}", e.getMessage());
        }
    }

    /** Se dispara al reaparecer (clon del jugador tras la muerte). */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        try {
            if (!event.isWasDeath()) return;
            if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

            List<ItemStack> rescued = SAVED.remove(event.getOriginal().getUUID());
            if (rescued == null || rescued.isEmpty()) return;

            for (ItemStack stack : rescued) {
                if (!newPlayer.getInventory().add(stack)) {
                    // Sin espacio en el inventario: lo dejamos a sus pies en vez de perderlo.
                    newPlayer.drop(stack, false);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneLoyalty restore error: {}", e.getMessage());
        }
    }
}
