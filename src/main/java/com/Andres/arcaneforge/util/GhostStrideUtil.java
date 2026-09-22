package com.Andres.arcaneforge.util;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/**
 * Punto unico para saber si una entidad lleva puesto Zancada Fantasma en los
 * pantalones. Lo usan tres mixins distintos (ignorar placas/trampas, quitar
 * sonido y vibracion de pasos, ocultar el nametag), todos de solo lectura y
 * sin efectos secundarios: seguro de llamar tanto en cliente como servidor.
 */
public final class GhostStrideUtil {

    private GhostStrideUtil() {}

    private static final ResourceKey<Enchantment> ZANCADA_FANTASMA_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "zancada_fantasma"));

    public static boolean isActive(LivingEntity entity) {
        try {
            ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
            if (legs.isEmpty()) return false;

            var registry = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(ZANCADA_FANTASMA_KEY);
            if (opt.isEmpty()) return false;

            ItemEnchantments enchants = legs.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get()) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
