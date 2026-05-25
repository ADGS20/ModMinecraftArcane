package com.Andres.arcaneforge.mixin;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.serialization.Codec;

@Mixin({ItemEnchantments.Mutable.class, ItemEnchantments.class})
public class EnchantmentLimitMixin {

    /**
     * Rompe el límite de la aplicación del encantamiento (Math.min).
     */
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int bypassMathMinCap(int level, int vanillaMax) {
        return level; // ¡Devuelve tu nivel 10,000 sin recortar nada!
    }

    /**
     * Rompe el límite del sistema de guardado (NBT Codec) para que el ítem no se corrompa.
     */
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"))
    private static Codec<Integer> bypassCodecCap(int min, int max) {
        return Codec.intRange(1, 10000);
    }
}