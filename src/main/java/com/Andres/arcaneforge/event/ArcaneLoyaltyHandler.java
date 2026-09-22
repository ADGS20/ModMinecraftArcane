package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
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

    private static final ResourceKey<Enchantment> ALMA_PERDURABLE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "alma_perdurable"));

    /** Items rescatados de la muerte, en espera de que el jugador reaparezca. */
    private static final Map<UUID, List<ItemStack>> SAVED = new ConcurrentHashMap<>();

    /** Nivel/progreso/total de XP justo antes de morir, para restaurarlo intacto al reaparecer. */
    private record SavedXp(int level, float progress, int total) {}
    private static final Map<UUID, SavedXp> SAVED_XP = new ConcurrentHashMap<>();

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
            if (rescued != null && !rescued.isEmpty()) {
                for (ItemStack stack : rescued) {
                    if (!newPlayer.getInventory().add(stack)) {
                        // Sin espacio en el inventario: lo dejamos a sus pies en vez de perderlo.
                        newPlayer.drop(stack, false);
                    }
                }
            }

            SavedXp xp = SAVED_XP.remove(event.getOriginal().getUUID());
            ArcaneForge.LOGGER.info("[ALMA-PERD] onPlayerClone uuid={} foundSavedXp={}", event.getOriginal().getUUID(), xp != null);
            if (xp != null) {
                newPlayer.experienceLevel = xp.level();
                newPlayer.experienceProgress = xp.progress();
                newPlayer.totalExperience = xp.total();
                ArcaneForge.LOGGER.info("[ALMA-PERD] restored level={} progress={} total={}", xp.level(), xp.progress(), xp.total());
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ArcaneLoyalty restore error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALMA PERDURABLE — la experiencia tampoco se pierde al morir
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * LivingDeathEvent se dispara al PRINCIPIO de LivingEntity.die(), antes de
     * que dropAllDeathLoot() vacie el equipo del jugador (dropEquipment() corre
     * ANTES que dropExperience() en ese metodo). Por eso el chequeo de si lleva
     * Alma Perdurable puesto tiene que hacerse aqui y no en el evento de soltar
     * XP: para cuando ese otro evento llega, las 6 slots ya estan vacias y el
     * encantamiento nunca se detecta, sin importar que estuviera equipado.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            if (!hasAlmaPerdurable(sp)) return;

            SAVED_XP.put(sp.getUUID(), new SavedXp(sp.experienceLevel, sp.experienceProgress, sp.totalExperience));
            ArcaneForge.LOGGER.info("[ALMA-PERD] onLivingDeath player={} uuid={} guardado level={} progress={} total={}",
                    sp.getGameProfile().name(), sp.getUUID(), sp.experienceLevel, sp.experienceProgress, sp.totalExperience);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("AlmaPerdurable death-snapshot error: {}", e.getMessage());
        }
    }

    /**
     * Cancela las esferas de XP que Player.dropExperience() sueltamente
     * suelta al morir (hasta min(nivel*7, 100)), pero solo si onLivingDeath ya
     * guardo un snapshot para este jugador — si no lo guardo (no llevaba el
     * encantamiento puesto), la XP se comporta como en vanilla.
     */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            if (!SAVED_XP.containsKey(sp.getUUID())) return;

            event.setDroppedExperience(0);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("AlmaPerdurable drop error: {}", e.getMessage());
        }
    }

    private static final EquipmentSlot[] CHECK_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static boolean hasAlmaPerdurable(ServerPlayer player) {
        try {
            var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = reg.get(ALMA_PERDURABLE_KEY);
            if (opt.isEmpty()) {
                ArcaneForge.LOGGER.info("[ALMA-PERD] enchant not found in registry");
                return false;
            }
            for (EquipmentSlot slot : CHECK_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                int lvl = ench.getLevel(opt.get());
                ArcaneForge.LOGGER.info("[ALMA-PERD] slot={} item={} almaLevel={}", slot, stack.getItem(), lvl);
                if (lvl > 0) return true;
            }
            return false;
        } catch (Exception e) {
            ArcaneForge.LOGGER.info("[ALMA-PERD] hasAlmaPerdurable exception: {}", e.toString());
            return false;
        }
    }
}
