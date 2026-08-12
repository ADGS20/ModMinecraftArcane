package com.Andres.arcaneforge.miners;

import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Los materiales que la Vision Minera puede detectar, con el color de brillo
 * que le corresponde a cada uno (mismo color que usa el efecto Brillo cuando
 * el jugador esta en un equipo de scoreboard con ese color, que es como se
 * consigue el "contorno visible a traves de paredes" en vanilla).
 */
public enum OreFilter {
    ALL("todos", "Todos", null),
    IRON("hierro", "Hierro", ChatFormatting.WHITE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
    GOLD("oro", "Oro", ChatFormatting.YELLOW, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
    DIAMOND("diamante", "Diamante", ChatFormatting.AQUA, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
    LAPIS("lapislazuli", "Lapislazuli", ChatFormatting.DARK_BLUE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
    REDSTONE("redstone", "Redstone", ChatFormatting.RED, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
    NETHERITE("netherita", "Netherita", ChatFormatting.BLACK, Blocks.ANCIENT_DEBRIS),
    QUARTZ("cuarzo", "Cuarzo", ChatFormatting.GRAY, Blocks.NETHER_QUARTZ_ORE),
    COAL("carbon", "Carbon", ChatFormatting.DARK_GRAY, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
    EMERALD("esmeralda", "Esmeralda", ChatFormatting.GREEN, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
    COPPER("cobre", "Cobre", ChatFormatting.GOLD, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final Set<Block> blocks;

    OreFilter(String id, String displayName, ChatFormatting color, Block... blocks) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.blocks = Set.of(blocks);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
    public Set<Block> blocks() { return blocks; }

    private static final Map<String, OreFilter> BY_ID = new HashMap<>();
    private static final Map<Block, OreFilter> BY_BLOCK = new HashMap<>();
    static {
        for (OreFilter f : values()) {
            BY_ID.put(f.id, f);
            if (f != ALL) {
                for (Block b : f.blocks) BY_BLOCK.put(b, f);
            }
        }
    }

    public static OreFilter byId(String id) {
        return BY_ID.getOrDefault(id, ALL);
    }

    /** A que filtro pertenece este bloque (mineral), o null si no es ninguno de los detectables. */
    public static OreFilter classify(Block block) {
        return BY_BLOCK.get(block);
    }
}
