package com.Andres.arcaneforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bloques de descuento que el aldeano verde (Nitwit) entrega a cambio de
 * esmeraldas + item. Colocados cerca de la Forja Arcana, reducen su coste:
 * la rama MATERIAL abarata el coste de magic fuel, la rama XP abarata el
 * coste de niveles de experiencia. Reutilizan el modelo visual del Pedestal
 * o del Bloque de Poder segun la rama; lo que los distingue es su nombre
 * ("Nivel 1/2/3"), el brillo de encantado y el descuento que aplican.
 */
public class ArcaneDiscountBlock extends Block {

    public enum Branch { MATERIAL, XP }

    private final Branch branch;
    private final int tier;
    private final float discountFraction;

    public ArcaneDiscountBlock(Properties properties, Branch branch, int tier, float discountFraction) {
        super(properties);
        this.branch = branch;
        this.tier = tier;
        this.discountFraction = discountFraction;
    }

    public Branch getBranch() { return branch; }
    public int getTier() { return tier; }
    public float getDiscountFraction() { return discountFraction; }

    public static float getBestDiscount(Level level, BlockPos centerPos, Branch branch) {
        int radius = 3;
        float best = 0f;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof ArcaneDiscountBlock discountBlock && discountBlock.branch == branch) {
                        if (discountBlock.discountFraction > best) best = discountBlock.discountFraction;
                    }
                }
            }
        }
        return best;
    }
}
