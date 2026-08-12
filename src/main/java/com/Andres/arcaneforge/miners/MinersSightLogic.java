package com.Andres.arcaneforge.miners;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/** Logica compartida de la Vision Minera (estado guardado en el propio casco). */
public final class MinersSightLogic {

    public static final ResourceKey<Enchantment> MINERS_SIGHT_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "miners_sight"));

    private static final String ENABLED_KEY = "miners_sight_enabled";
    private static final String FILTER_KEY  = "miners_sight_filter";

    /** Radio base + por nivel del escaneo. Nivel 1 = 9, nivel 2 = 12, nivel 3 = 15. */
    private static final int BASE_RADIUS = 6;
    private static final int RADIUS_PER_LEVEL = 3;

    /**
     * Tope real de nivel para esta habilidad. IMPORTANTE: el sistema de la
     * Forja Arcana (ArcaneForgeBlockEntity.tryEnchant) IGNORA el max_level
     * del json de cada encantamiento y deja subir cualquiera hasta 15 (o 255
     * con Pedestal activo). Sin este tope aparte, un jugador podria subir
     * Vision Minera a nivel 255 y el radio de escaneo (6 + nivel*3) se
     * volveria gigantesco, congelando el servidor. getLevel() recorta
     * siempre al nivel real guardado en el item.
     */
    private static final int MAX_LEVEL = 3;

    /** Cuanta XP (puntos) cuesta cada ciclo de escaneo, por nivel del encantamiento. */
    private static final int XP_COST_PER_LEVEL = 1;

    private MinersSightLogic() {}

    public static int getLevel(Player player, ItemStack helmet) {
        if (helmet.isEmpty()) return 0;
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(MINERS_SIGHT_KEY);
            if (opt.isEmpty()) return 0;
            ItemEnchantments ench = helmet.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return Math.min(MAX_LEVEL, ench.getLevel(opt.get()));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int radiusForLevel(int level) {
        // Segunda capa de seguridad: aunque getLevel() ya recorta a MAX_LEVEL,
        // esto evita un escaneo descontrolado si algun dia se llama con un
        // nivel sin pasar por getLevel().
        int clamped = Math.min(level, MAX_LEVEL);
        return BASE_RADIUS + clamped * RADIUS_PER_LEVEL;
    }

    public static int xpCostForLevel(int level) {
        return Math.max(1, level * XP_COST_PER_LEVEL);
    }

    public static boolean isEnabled(ItemStack helmet) {
        if (helmet.isEmpty()) return false;
        CompoundTag tag = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(ENABLED_KEY).orElse(false);
    }

    public static void setEnabled(ItemStack helmet, boolean enabled) {
        if (helmet.isEmpty()) return;
        CompoundTag tag = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(ENABLED_KEY, enabled);
        helmet.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static OreFilter getFilter(ItemStack helmet) {
        if (helmet.isEmpty()) return OreFilter.ALL;
        CompoundTag tag = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return OreFilter.byId(tag.getString(FILTER_KEY).orElse(OreFilter.ALL.id()));
    }

    public static void setFilter(ItemStack helmet, OreFilter filter) {
        if (helmet.isEmpty()) return;
        CompoundTag tag = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(FILTER_KEY, filter.id());
        helmet.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
