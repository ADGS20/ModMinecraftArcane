package com.Andres.arcaneforge.util;

import com.Andres.arcaneforge.component.ArcaneEnchantments;
import com.Andres.arcaneforge.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * ArcaneEnchantHelper — punto único para leer y escribir encantamientos arcanos.
 *
 * CORRECCIÓN 26.1.2:
 *   ResourceLocation → Identifier
 *   ResourceKey.location() → ResourceKey.identifier()
 */
public final class ArcaneEnchantHelper {

    private ArcaneEnchantHelper() {}

    /**
     * Obtiene el nivel REAL de un encantamiento en un ítem.
     * Busca primero en el componente arcano (niveles hasta 100.000).
     * Si no está, busca en el componente vanilla (niveles hasta 255).
     */
    public static int getArcaneLevel(ItemStack stack, Identifier enchantmentId) {
        ArcaneEnchantments arcane = stack.getOrDefault(
                ModDataComponents.ARCANE_ENCHANTMENTS.get(),
                ArcaneEnchantments.empty()
        );
        int arcaneLevel = arcane.getLevel(enchantmentId);
        if (arcaneLevel > 0) return arcaneLevel;

        // Fallback vanilla
        ItemEnchantments vanillaEnchants = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : vanillaEnchants.entrySet()) {
            var keyOpt = entry.getKey().unwrapKey();
            if (keyOpt.isPresent()) {
                // CORRECCIÓN: .location() → .identifier() en ResourceKey
                Identifier id = keyOpt.get().identifier();
                if (id.equals(enchantmentId)) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
    }

    /**
     * Aplica un encantamiento arcano al ítem.
     *
     * Nivel <= 255 → solo vanilla.
     * Nivel > 255  → vanilla queda en 255, nivel real en componente arcano.
     * Nivel == 0   → elimina de ambos.
     */
    public static void applyArcaneEnchantment(ItemStack stack,
                                              Holder<Enchantment> enchantHolder,
                                              int level) {
        var keyOpt = enchantHolder.unwrapKey();
        if (keyOpt.isEmpty()) return;
        // CORRECCIÓN: .location() → .identifier()
        Identifier enchId = keyOpt.get().identifier();

        if (level <= 0) {
            removeEnchantment(stack, enchantHolder, enchId);
            return;
        }

        // Vanilla: cap a 255
        int vanillaLevel = Math.min(level, 255);
        ItemEnchantments currentVanilla = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentVanilla);
        mutable.set(enchantHolder, vanillaLevel);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        if (level > 255) {
            // Guardar nivel real en componente arcano
            ArcaneEnchantments current = stack.getOrDefault(
                    ModDataComponents.ARCANE_ENCHANTMENTS.get(),
                    ArcaneEnchantments.empty()
            );
            stack.set(ModDataComponents.ARCANE_ENCHANTMENTS.get(),
                    current.withLevel(enchId, level));
        } else {
            // Limpiar del componente arcano si existía antes
            ArcaneEnchantments current = stack.get(
                    ModDataComponents.ARCANE_ENCHANTMENTS.get());
            if (current != null && current.getLevel(enchId) > 0) {
                ArcaneEnchantments updated = current.withLevel(enchId, 0);
                if (updated.isEmpty()) {
                    stack.remove(ModDataComponents.ARCANE_ENCHANTMENTS.get());
                } else {
                    stack.set(ModDataComponents.ARCANE_ENCHANTMENTS.get(), updated);
                }
            }
        }
    }

    private static void removeEnchantment(ItemStack stack,
                                          Holder<Enchantment> holder,
                                          Identifier enchId) {
        // Vanilla
        ItemEnchantments current = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!current.isEmpty()) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
            mutable.set(holder, 0);
            ItemEnchantments updated = mutable.toImmutable();
            if (updated.isEmpty()) {
                stack.remove(DataComponents.ENCHANTMENTS);
            } else {
                stack.set(DataComponents.ENCHANTMENTS, updated);
            }
        }
        // Arcano
        ArcaneEnchantments arcane = stack.get(
                ModDataComponents.ARCANE_ENCHANTMENTS.get());
        if (arcane != null && arcane.getLevel(enchId) > 0) {
            ArcaneEnchantments newArcane = arcane.withLevel(enchId, 0);
            if (newArcane.isEmpty()) {
                stack.remove(ModDataComponents.ARCANE_ENCHANTMENTS.get());
            } else {
                stack.set(ModDataComponents.ARCANE_ENCHANTMENTS.get(), newArcane);
            }
        }
    }

    public static boolean hasArcaneEnchantment(ItemStack stack, Identifier enchId) {
        ArcaneEnchantments arcane = stack.get(
                ModDataComponents.ARCANE_ENCHANTMENTS.get());
        return arcane != null && arcane.getLevel(enchId) > 0;
    }
}