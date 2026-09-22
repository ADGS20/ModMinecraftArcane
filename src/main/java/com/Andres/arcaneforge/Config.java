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
    public static final int MAX_ENCHANTMENT_LEVEL = 10000;

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
    // Luna Oscura/Roja (evento mundial escalable)
    // ════════════════════════════════════

    /** Cada cuántos días de juego avanza una ronda de la Luna. */
    public static final int MOON_INTERVAL_DAYS = 10;

    /**
     * Duración de la ventana de peligro extra tras subir de ronda, en ticks
     * de juego (20 ticks = 1 segundo; 24000 = un día completo de juego).
     * Esta versión de Minecraft ya no expone getDayTime()/isDay() en
     * ServerLevel — de hecho, tras revisar el código fuente, el día/noche
     * ahora corre por un sistema de "WorldClock" totalmente nuevo y aún
     * experimental (net.minecraft.world.clock) — así que la ventana se mide
     * en ticks de juego absolutos en vez de "hasta el próximo amanecer", y
     * no depende para nada del ciclo real de sol/luna. El valor son 2 días
     * completos (el doble de antes): la duración real de la "noche" en sí
     * no se toca (arriesgado, sistema nuevo y frágil), pero el PELIGRO de la
     * Luna (caza, horda, cielo rojo) dura el doble sea de día o de noche.
     */
    public static final long MOON_DURATION_TICKS = 48000L;

    /** Probabilidad base (ronda 1) de que un mob hostil salga con equipo encantado. */
    public static final double MOON_BASE_GEAR_CHANCE = 0.15;

    /** Cuánto sube esa probabilidad por cada ronda. */
    public static final double MOON_GEAR_CHANCE_PER_ROUND = 0.03;

    /** Tope de esa probabilidad (fuera de la noche de Luna activa). */
    public static final double MOON_MAX_GEAR_CHANCE = 0.55;

    /** Probabilidad base de que un mob equipado sea ademas un "Heraldo" (elite/sub-jefe). */
    public static final double MOON_BASE_ELITE_CHANCE = 0.03;

    /** Probabilidad de Heraldo mientras la Luna esta activa (esa noche concreta). */
    public static final double MOON_ACTIVE_ELITE_CHANCE = 0.18;

    /** Días de juego que adelanta el Fragmento Lunar al consumirse. */
    public static final int MOON_ACCELERATOR_DAYS_SKIPPED = 3;

    /** Días de juego de espera entre dos usos del Fragmento Lunar. */
    public static final int MOON_ACCELERATOR_COOLDOWN_DAYS = 1;

    /** Radio (en chunks) en el que un mob detecta y persigue directamente al jugador durante la Luna activa. */
    public static final int MOON_HUNT_RADIUS_CHUNKS = 15;

    /** Cada cuantos ticks se vuelve a forzar el objetivo de todo mob hostil dentro del radio de caza (30 = 1.5s). */
    public static final int MOON_HUNT_RETARGET_TICKS = 30;

    /** Cada cuantos ticks aparece una nueva oleada de horda junto a cada jugador (100 = 5s). */
    public static final int MOON_HORDE_INTERVAL_TICKS = 100;

    /** Cuantos mobs nuevos intenta añadir cada oleada de horda por jugador. */
    public static final int MOON_HORDE_SPAWNS_PER_WAVE = 4;

    /**
     * Tope de mobs hostiles ya cerca de un jugador (dentro de
     * MOON_HORDE_CAP_RADIUS) antes de dejar de sumar mas oleadas — la horda
     * se spawnea a mano con EntityType.spawn(...), que NO pasa por el limite
     * natural de mobs de vainilla (NaturalSpawner), asi que sin este tope
     * propio la cantidad crecería sin fin.
     */
    public static final int MOON_HORDE_MAX_PER_PLAYER = 40;

    /** Radio (en bloques) en el que se cuentan los mobs ya cercanos para el tope de horda. */
    public static final double MOON_HORDE_CAP_RADIUS = 64.0;

    /** Distancia minima/maxima (en bloques) a la que aparece cada mob de una oleada de horda. */
    public static final double MOON_HORDE_MIN_DIST = 20.0;
    public static final double MOON_HORDE_MAX_DIST = 40.0;

    /** Ronda minima a partir de la cual un Heraldo puede romper bloques naturales para llegar al jugador. */
    public static final int MOON_HERALDO_DIG_MIN_ROUND = 4;

    /** Cada cuantos ticks un Heraldo bloqueado intenta romper el bloque que lo detiene. */
    public static final int MOON_HERALDO_DIG_INTERVAL_TICKS = 10;

    /** Cuantos mobs aparecen por defecto con /arcaneforge moon wave si no se especifica cantidad. */
    public static final int MOON_WAVE_DEFAULT_COUNT = 8;

    /** Tope duro de mobs que puede pedir de una vez /arcaneforge moon wave (evitar spam/lag del comando). */
    public static final int MOON_WAVE_MAX_COUNT = 300;

    /** Ronda maxima que acepta /arcaneforge moon wave — por encima de esto el nivel de encantamiento ya no sube (tope real 255), pero salud/daño de Heraldo si siguen escalando. */
    public static final int MOON_WAVE_MAX_ROUND = 50;

    /**
     * Tope real del multiplicador de vida/daño de un Heraldo (ver
     * ArcaneMoonLogic.equipMob). Antes se topaba en la ronda 10 (mismo punto
     * donde el nivel de encantamiento ya llega a 255) y nunca subia mas —
     * asi que rondas "administrador" mas altas (via /arcaneforge moon wave)
     * no se sentian mas peligrosas de verdad. Ahora sigue subiendo mucho mas
     * alla de la ronda 10.
     */
    public static final double MOON_ELITE_HEALTH_MULT_CAP = 12.0;
    public static final double MOON_ELITE_DAMAGE_MULT_CAP = 10.0;

    /**
     * A partir de esta ronda, un Heraldo sale con armadura COMPLETA (casco,
     * pecho, piernas, botas) en vez de solo el peto — vainilla ya reduce
     * mucho el daño recibido solo por llevar armadura puesta, y sin esto un
     * Heraldo de ronda alta se sentia demasiado fragil pese a su enchant
     * altisimo (el enchant sube el daño que HACE, no lo dificil que es
     * matarlo).
     */
    public static final int MOON_HERALDO_FULL_ARMOR_MIN_ROUND = 3;

    /** A partir de esta ronda, un Heraldo lleva un Totem de la Inmortalidad en la mano secundaria. */
    public static final int MOON_HERALDO_TOTEM_MIN_ROUND = 6;

    // ════════════════════════════════════
    // Generador Arcano de Gólems
    // ════════════════════════════════════

    /** Cada cuántos ticks intenta producir un gólem el generador (6000 = 5 minutos reales). */
    public static final int GOLEM_GENERATOR_INTERVAL_TICKS = 6000;

    /** Bloques de Hierro por gólem (el doble de la receta vainilla real: 4). */
    public static final int GOLEM_GENERATOR_IRON_BLOCKS = 8;

    /** Calabazas Talladas por gólem (el doble de la receta vainilla real: 1). */
    public static final int GOLEM_GENERATOR_PUMPKINS = 2;

    /** Bloques de Nieve por gólem de nieve (el doble de la receta vainilla real: 2). */
    public static final int GOLEM_GENERATOR_SNOW_BLOCKS = 4;

    /** Tope de gólems ya generados cerca del bloque antes de pausar la producción (anti-spam/lag). */
    public static final int GOLEM_GENERATOR_MAX_NEARBY = 4;

    /** Radio (en bloques) en el que se cuentan los gólems ya generados para el tope anterior. */
    public static final int GOLEM_GENERATOR_NEARBY_RADIUS = 16;

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
     *
     * @param currentLevel El nivel actual del encantamiento en el item
     * @param targetLevel El nivel de encantamiento deseado
     * @param bookshelfCount   Número de librerías cercanas
     * @return El costo en puntos de combustible mágico
     */
    public static int calculateEnchantCost(int currentLevel, int targetLevel, int bookshelfCount) {
        int baseCost = ((currentLevel + targetLevel) * 2) - 1;
        int reduction = bookshelfCount / BOOKSHELVES_PER_REDUCTION;
        return Math.max(1, baseCost - reduction);
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
