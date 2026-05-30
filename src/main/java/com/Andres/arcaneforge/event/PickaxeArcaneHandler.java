package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Maneja todas las combinaciones de encantamientos arcanos en picos:
 *
 *  Sólo Fortune        → vanilla (ya lo maneja MC)
 *  Sólo Silk Touch     → vanilla (ya lo maneja MC)
 *  Fortune + Silk Touch → Silk Touch activo + Fortune multiplica los drops
 *  Arcane Smelting     → convierte minerales a lingotes
 *  Arcane Smelting + Fortune → convierte Y multiplica
 *  Arcane Smelting + Silk Touch → arcane smelting tiene prioridad (smelts silk touch drops)
 *  Arcane Smelting + Fortune + Silk Touch → smelts todo y multiplica por fortune
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class PickaxeArcaneHandler {

    private static final ResourceKey<Enchantment> ARCANE_SMELT_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_smelting"));

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() == null || event.getLevel().isClientSide()) return;
        if (!(event.getBreaker() instanceof Player player)) return;

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || event.getDrops().isEmpty()) return;

        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            int fortuneLevel    = getEnchantLevel(registry, enchants, Enchantments.FORTUNE);
            int silkTouchLevel  = getEnchantLevel(registry, enchants, Enchantments.SILK_TOUCH);
            Optional<Holder.Reference<Enchantment>> smeltOpt = registry.get(ARCANE_SMELT_KEY);
            boolean hasArcaneSmelting = smeltOpt.isPresent() && enchants.getLevel(smeltOpt.get()) > 0;

            RandomSource random = event.getLevel().getRandom();

            // ── Caso 1: Arcane Smelting activo ──────────────────────────────────
            // Prioridad máxima: convierte minerales a lingotes + multiplica por Fortune
            if (hasArcaneSmelting) {
                smeltDrops(event, fortuneLevel, random);
                return; // arcane smelting ya aplicó fortune internamente
            }

            // ── Caso 2: Fortune + Silk Touch fusionados (SIN Arcane Smelting) ────
            // Silk Touch ya actuó en vanilla → los drops son los bloques en sí
            // Aquí multiplicamos esos drops por Fortune
            if (fortuneLevel > 0 && silkTouchLevel > 0) {
                multiplyDrops(event, fortuneLevel, random);
            }

            // ── Caso 3: Sólo Fortune / Sólo Silk Touch ───────────────────────────
            // Vanilla ya los maneja; no necesitamos hacer nada extra aquí.

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("PickaxeArcaneHandler error: {}", e.getMessage());
        }
    }

    // ─── Arcane Smelting: convierte minerales a lingotes + Fortune multiplier ───

    private static void smeltDrops(BlockDropsEvent event, int fortuneLevel, RandomSource random) {
        event.getDrops().forEach(itemEntity -> {
            ItemStack drop = itemEntity.getItem();
            ItemStack smelted = getSmeltingResult(drop, random, fortuneLevel);
            if (!smelted.isEmpty()) {
                itemEntity.setItem(smelted);
            }
        });
    }

    private static ItemStack getSmeltingResult(ItemStack original, RandomSource random, int fortuneLevel) {
        ItemStack result = ItemStack.EMPTY;

        // Hierro
        if (original.is(Items.RAW_IRON)   ||
            original.is(Items.IRON_ORE)   ||
            original.is(Items.DEEPSLATE_IRON_ORE)) {
            result = new ItemStack(Items.IRON_INGOT);
        }
        // Oro
        else if (original.is(Items.RAW_GOLD)        ||
                 original.is(Items.GOLD_ORE)         ||
                 original.is(Items.DEEPSLATE_GOLD_ORE) ||
                 original.is(Items.NETHER_GOLD_ORE)) {
            result = new ItemStack(Items.GOLD_INGOT);
        }
        // Cobre
        else if (original.is(Items.RAW_COPPER)        ||
                 original.is(Items.COPPER_ORE)         ||
                 original.is(Items.DEEPSLATE_COPPER_ORE)) {
            result = new ItemStack(Items.COPPER_INGOT);
        }

        if (!result.isEmpty()) {
            int count = original.getCount();
            if (fortuneLevel > 0) {
                count *= (1 + random.nextInt(fortuneLevel + 1));
            }
            result.setCount(Math.min(count, result.getMaxStackSize()));
        }

        return result;
    }

    // ─── Fortune + Silk Touch: multiplica los drops ya existentes ───────────────

    /**
     * Cuando el pico tiene Silk Touch + Fortune simultáneamente (posible con Arcane Forge),
     * vanilla sólo aplica Silk Touch. Aquí añadimos drops extra multiplicados por Fortune.
     */
    private static void multiplyDrops(BlockDropsEvent event, int fortuneLevel, RandomSource random) {
        List<ItemEntity> extras = new ArrayList<>();

        for (ItemEntity entity : event.getDrops()) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;

            int extraCount = random.nextInt(fortuneLevel + 1); // 0 … fortuneLevel
            if (extraCount <= 0) continue;

            int remaining = extraCount * stack.getCount();
            int maxStack  = stack.getMaxStackSize();

            while (remaining > 0) {
                int split = Math.min(maxStack, remaining);
                ItemStack extra = stack.copy();
                extra.setCount(split);
                ItemEntity extraEntity = new ItemEntity(
                        entity.level(),
                        entity.getX(), entity.getY(), entity.getZ(),
                        extra);
                extraEntity.setDefaultPickUpDelay();
                extras.add(extraEntity);
                remaining -= split;
            }
        }

        if (!extras.isEmpty()) {
            event.getDrops().addAll(extras);
            ArcaneForge.LOGGER.debug(
                    "PickaxeArcaneHandler: Silk Touch + Fortune {} → +{} drop entities",
                    fortuneLevel, extras.size());
        }
    }

    // ─── Utilidad ────────────────────────────────────────────────────────────────

    private static int getEnchantLevel(
            net.minecraft.core.Registry<Enchantment> registry,
            ItemEnchantments enchants,
            ResourceKey<Enchantment> key) {
        try {
            Optional<Holder.Reference<Enchantment>> opt = registry.get(key);
            return opt.map(enchants::getLevel).orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }
}
