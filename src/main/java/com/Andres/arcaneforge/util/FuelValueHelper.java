package com.Andres.arcaneforge.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

public class FuelValueHelper {
    private static final Map<Item, Integer> FUEL_VALUES = new HashMap<>();

    static {
        initializeFuelValues();
    }

    private static void initializeFuelValues() {
        // VALORES ALTOS - Ítems raros y legendarios
        FUEL_VALUES.put(Items.NETHER_STAR, 500);
        FUEL_VALUES.put(Items.DRAGON_EGG, 1000);
        FUEL_VALUES.put(Items.DIAMOND, 100);
        FUEL_VALUES.put(Items.EMERALD, 150);
        FUEL_VALUES.put(Items.ECHO_SHARD, 200);
        FUEL_VALUES.put(Items.NETHERITE_INGOT, 300);

        // VALORES MEDIOS - Metales comunes y componentes mísitcos
        FUEL_VALUES.put(Items.GOLD_INGOT, 25);
        FUEL_VALUES.put(Items.IRON_INGOT, 15);
        FUEL_VALUES.put(Items.COPPER_INGOT, 10);
        FUEL_VALUES.put(Items.LAPIS_LAZULI, 20);
        FUEL_VALUES.put(Items.BLAZE_ROD, 30);
        FUEL_VALUES.put(Items.AMETHYST_SHARD, 12);

        // VALORES BAJOS - Básicos
        FUEL_VALUES.put(Items.COAL, 5);
        FUEL_VALUES.put(Items.CHARCOAL, 5);
    }

    public static int getFuelValue(Item item) {
        if (FUEL_VALUES.containsKey(item)) {
            return FUEL_VALUES.get(item);
        }
        return calculateDefaultFuelValue(item);
    }

    public static boolean isFuel(Item item) {
        return getFuelValue(item) > 0;
    }

    private static int calculateDefaultFuelValue(Item item) {
        String itemName = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        if (itemName.contains("block")) return 10;
        if (itemName.contains("ore")) return 5;
        if (itemName.contains("ingot")) return 8;
        if (itemName.contains("raw")) return 4;
        if (itemName.contains("sword") || itemName.contains("pickaxe") || itemName.contains("axe")) return 6;
        return 1; // Cualquier otro objeto da como mínimo 1 punto de energía
    }
}