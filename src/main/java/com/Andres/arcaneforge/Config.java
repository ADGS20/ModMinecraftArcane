package com.Andres.arcaneforge;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Arcane Forge Configuration — VERSIÓN COMPLETA para NeoForge 26.1.2.65-beta.
 *
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  IMPORTANTE: Este archivo REEMPLAZA cualquier Config.java       ║
 * ║  existente en el paquete com.Andres.arcaneforge.               ║
 * ║  Si tu proyecto ya tiene un Config.java, BÓRRALO y usa este.   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Contiene TODOS los campos y métodos que referencian los demás archivos:
 *   - MAX_LINKED_CHESTS
 *   - BOOKSHELF_RADIUS_XZ, BOOKSHELF_RADIUS_Y
 *   - MAX_BOOKSHELF_COUNT
 *   - MAX_ENCHANTMENT_LEVEL
 *   - SYNC_INTERVAL_TICKS
 *   - getFuelValue(Item)
 *   - calculateEnchantCost(int, int)
 *
 * Sistema de Combustible Mágico:
 *   Cualquier item registrado sirve como combustible. Cada item tiene un
 *   valor en "puntos mágicos". Items no registrados = 0 puntos (no sirven).
 */
public class Config {

    // ════════════════════════════════════
    // Límites generales
    // ════════════════════════════════════

    /** Máximo número de cofres que se pueden vincular a una forja. */
    public static final int MAX_LINKED_CHESTS = 256;

    /** Nivel máximo de encantamiento permitido (efectivamente sin límite). */
    public static final int MAX_ENCHANTMENT_LEVEL = 1000;

    /** Reducción de costo por cada N librerías cercanas. */
    public static final int BOOKSHELVES_PER_REDUCTION = 5;

    /** Cap máximo de librerías que se cuentan (0 = sin cap). */
    public static final int MAX_BOOKSHELF_COUNT = 50;

    /** Radio de búsqueda de librerías (en bloques desde la forja). */
    public static final int BOOKSHELF_RADIUS_XZ = 5;
    public static final int BOOKSHELF_RADIUS_Y = 2;

    /** Intervalo de sincronización servidor→cliente (en ticks). 20 = 1 segundo. */
    public static final int SYNC_INTERVAL_TICKS = 20;

    /** Radio de búsqueda del Pedestal Arcano (en bloques). */
    public static final int PEDESTAL_RANGE = 3;

    // ════════════════════════════════════
    // Sistema de Combustible Mágico
    // ════════════════════════════════════

    /** Mapa de Item → valor en puntos mágicos. */
    private static final Map<Item, Integer> FUEL_VALUES = new HashMap<>();

    static {
        // ── Valores base (según especificación del usuario) ──
        FUEL_VALUES.put(Items.LAPIS_LAZULI, 1);
        FUEL_VALUES.put(Items.REDSTONE, 2);
        FUEL_VALUES.put(Items.GOLD_INGOT, 5);
        FUEL_VALUES.put(Items.DIAMOND, 10);
        FUEL_VALUES.put(Items.EMERALD, 15);
        FUEL_VALUES.put(Items.NETHERITE_INGOT, 50);
        FUEL_VALUES.put(Items.NETHER_STAR, 500);

        // ── Minerales adicionales ──
        FUEL_VALUES.put(Items.COAL, 1);
        FUEL_VALUES.put(Items.CHARCOAL, 1);
        FUEL_VALUES.put(Items.COPPER_INGOT, 1);
        FUEL_VALUES.put(Items.IRON_INGOT, 3);
        FUEL_VALUES.put(Items.AMETHYST_SHARD, 2);
        FUEL_VALUES.put(Items.QUARTZ, 2);
        FUEL_VALUES.put(Items.PRISMARINE_SHARD, 2);
        FUEL_VALUES.put(Items.PRISMARINE_CRYSTALS, 3);

        // ── Bloques de mineral (9x el valor del ingot) ──
        FUEL_VALUES.put(Items.LAPIS_BLOCK, 9);
        FUEL_VALUES.put(Items.REDSTONE_BLOCK, 18);
        FUEL_VALUES.put(Items.COAL_BLOCK, 9);
        FUEL_VALUES.put(Items.COPPER_BLOCK, 9);
        FUEL_VALUES.put(Items.IRON_BLOCK, 27);
        FUEL_VALUES.put(Items.GOLD_BLOCK, 45);
        FUEL_VALUES.put(Items.EMERALD_BLOCK, 135);
        FUEL_VALUES.put(Items.DIAMOND_BLOCK, 90);
        FUEL_VALUES.put(Items.NETHERITE_BLOCK, 450);
        FUEL_VALUES.put(Items.AMETHYST_BLOCK, 8);

        // ── Items especiales ──
        FUEL_VALUES.put(Items.EXPERIENCE_BOTTLE, 5);
        FUEL_VALUES.put(Items.BLAZE_POWDER, 3);
        FUEL_VALUES.put(Items.BLAZE_ROD, 6);
        FUEL_VALUES.put(Items.ENDER_PEARL, 4);
        FUEL_VALUES.put(Items.ENDER_EYE, 8);
        FUEL_VALUES.put(Items.GHAST_TEAR, 10);
        FUEL_VALUES.put(Items.ECHO_SHARD, 15);
    }

    /**
     * Obtiene el valor de combustible mágico de un item.
     * @param item El item a consultar
     * @return puntos mágicos (0 si el item no es combustible)
     */
    public static int getFuelValue(Item item) {
        return FUEL_VALUES.getOrDefault(item, 0);
    }

    /**
     * Comprueba si un item puede usarse como combustible mágico.
     * @param item El item a verificar
     * @return true si tiene valor de combustible > 0
     */
    public static boolean isMagicFuel(Item item) {
        return FUEL_VALUES.containsKey(item) && FUEL_VALUES.get(item) > 0;
    }

    /**
     * Fórmula de costo de encantamiento.
     *
     * costo = (nivelActual + nivelDeseado) * 2 - 1, reducido por librerías.
     * Mínimo: 1 punto de combustible mágico.
     * El costo aumenta exponencialmente para niveles altos.
     *
     * @param currentLevel El nivel actual del encantamiento en el item
     * @param targetLevel El nivel de encantamiento deseado
     * @param bookshelfCount   Número de librerías cercanas
     * @return El costo en puntos de combustible mágico
     */
    public static int calculateEnchantCost(int currentLevel, int targetLevel, int bookshelfCount) {
        // Costo base: suma de niveles actuales y deseados
        int baseCost = ((currentLevel + targetLevel) * 2) - 1;
        
        // Reducción por librerías
        int reduction = bookshelfCount / BOOKSHELVES_PER_REDUCTION;
        int costAfterReduction = Math.max(1, baseCost - reduction);
        
        // Multiplicador progresivo para niveles altos (cada vez más caro)
        float multiplier = 1.0f;
        if (targetLevel > 50) {
            multiplier = 1.5f;
        }
        if (targetLevel > 100) {
            multiplier = 2.0f;
        }
        if (targetLevel > 200) {
            multiplier = 3.0f;
        }
        if (targetLevel > 500) {
            multiplier = 5.0f;
        }
        if (targetLevel > 800) {
            multiplier = 8.0f;
        }
        
        return Math.max(1, (int)(costAfterReduction * multiplier));
    }
    
    /**
     * Sobrecarga para compatibilidad (asume nivel actual = 0)
     */
    public static int calculateEnchantCost(int targetLevel, int bookshelfCount) {
        return calculateEnchantCost(0, targetLevel, bookshelfCount);
    }

    /**
     * Verifica si hay un Pedestal Arcano activo cerca y aplica multiplicador de costo.
     * Si hay pedestal, permite niveles más altos pero aumenta el costo.
     * 
     * @param baseCost El costo base calculado
     * @param hasActivePedestal Si hay un pedestal arcano activo cerca
     * @param targetLevel El nivel objetivo del encantamiento
     * @return El costo final ajustado
     */
    public static int calculateEnchantCostWithPedestal(int baseCost, boolean hasActivePedestal, int targetLevel) {
        if (!hasActivePedestal) {
            // Sin pedestal: solo permite hasta nivel 30 (límite vanilla mejorado)
            // El costo se mantiene igual, pero la UI debería limitar el nivel máximo
            return baseCost;
        }
        
        // Con pedestal: permite niveles superiores pero con costo aumentado
        // Multiplicador progresivo: más nivel = más caro exponencialmente
        float multiplier = 1.0f;
        
        if (targetLevel > 30) {
            // Nivel 31-50: 1.5x de costo
            multiplier = 1.5f;
        }
        if (targetLevel > 50) {
            // Nivel 51-100: 2.0x de costo
            multiplier = 2.0f;
        }
        if (targetLevel > 100) {
            // Nivel 101+: 3.0x de costo
            multiplier = 3.0f;
        }
        
        return Math.max(1, (int)(baseCost * multiplier));
    }

    /**
     * Obtiene una copia del mapa completo de valores de combustible.
     * Útil para mostrar en la GUI la lista de items aceptados.
     * @return Copia del mapa Item → puntos mágicos
     */
    public static Map<Item, Integer> getAllFuelValues() {
        return new HashMap<>(FUEL_VALUES);
    }
}
