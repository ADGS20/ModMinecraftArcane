package com.Andres.arcaneforge.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin sobre ItemEnchantments (la clase inmutable / Codec).
 *
 * PROBLEMA ORIGINAL:
 *   El Codec estático de ItemEnchantments usa intRange(1, 255).
 *   Cuando Minecraft guarda el ítem en NBT, el valor del nivel se valida
 *   contra ese Codec. Si el nivel es > 255, el Codec lanza un error
 *   y el ítem queda con nivel 0 o corrompe el guardado.
 *
 * SOLUCIÓN:
 *   @Redirect en <clinit> intercepta la llamada a Codec.intRange(1, 255)
 *   y la reemplaza por Codec.intRange(1, 10000).
 *   Esto hace que el sistema de guardado acepte niveles hasta 10,000.
 *
 * ERRORES DEL MIXIN ORIGINAL:
 *   1. @Mixin({ItemEnchantments.Mutable.class, ItemEnchantments.class})
 *      → Los @Mixin con múltiples clases NO pueden tener @Redirect en <clinit>
 *        porque el <clinit> es DIFERENTE para cada clase. Mixin no sabe a cuál
 *        aplicar el redirect. Resultado: el mixin no hace nada o crashea.
 *      CORRECCIÓN: Un @Mixin separado por clase.
 *
 *   2. El target "Ljava/lang/Math;min(II)I" no existe en ItemEnchantments.
 *      El código vanilla usa Mth.clamp(), no Math.min().
 *      CORRECCIÓN: Ver EnchantmentLevelMixin.java para el método set().
 */
@Mixin(ItemEnchantments.class)
public class ItemEnchantmentsMixin {

    /**
     * Redirige la creación del Codec de nivel de encantamiento.
     *
     * En el <clinit> de ItemEnchantments, Mojang hace:
     *   LEVEL_CODEC = Codec.intRange(1, 255)
     * Nosotros la interceptamos y devolvemos:
     *   Codec.intRange(1, 10000)
     *
     * Esto afecta SOLO al guardado/cargado de ítems en NBT.
     * El límite de aplicación (método set) se corrige por separado.
     */
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<Integer> expandEnchantmentLevelCodec(int min, int max) {
        // Ignoramos el max=255 de Mojang. 10,000 es el nuevo límite.
        // El min sigue siendo 1 (nivel 0 significaría eliminar el encantamiento).
        return Codec.intRange(1, 10_000);
    }
}