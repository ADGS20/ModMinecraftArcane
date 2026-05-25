package com.Andres.arcaneforge.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin sobre ItemEnchantments.Mutable (la clase editable).
 *
 * PROBLEMA:
 *   El método set(Holder<Enchantment>, int level) en ItemEnchantments.Mutable
 *   llama a Mth.clamp(level, 0, 255) antes de guardar el nivel.
 *   Aunque el Codec ya acepte 10,000, si set() hace clamp a 255,
 *   el encantamiento se aplica con nivel 255 en el ítem (no con 10,000).
 *
 * EN MINECRAFT 1.21.1:
 *   El campo interno es Object2IntOpenHashMap<Holder<Enchantment>> (int, no byte).
 *   El único límite es el Mth.clamp(level, 0, 255).
 *   No hay cast a byte — solo el clamp.
 *
 * SOLUCIÓN:
 *   @Redirect en el método set() intercepta la llamada a Mth.clamp
 *   y devuelve el nivel SIN recortar.
 *
 * ERRORES DEL MIXIN ORIGINAL (EnchantmentLimitMixin):
 *   1. Target incorrecto: "@At(INVOKE, target = Math.min(II)I)"
 *      → El código vanilla NO usa Math.min en este contexto. Usa Mth.clamp.
 *      → Con el target equivocado, el @Redirect nunca se dispara. El mixin
 *        "compila" pero no hace nada en runtime.
 *
 *   2. @Mixin sobre ambas clases a la vez: imposible con Redirect en métodos
 *      de instancia que tienen nombres diferentes en cada clase.
 *
 * NOTA SOBRE forceApplyEnchantment() en ArcaneForgeBlockEntity:
 *   Tu BlockEntity ya usa ItemEnchantments.Mutable.set() vía mutable.set(holder, level).
 *   Con este Mixin activo, ese set() ya NO recortará a 255.
 *   NO necesitas cambiar el BlockEntity — el Mixin lo corrige transparentemente.
 */
@Mixin(ItemEnchantments.Mutable.class)
public class EnchantmentLevelMixin {

    /**
     * Intercepta Mth.clamp(level, 0, 255) dentro de set() y devuelve el nivel sin modificar.
     *
     * Cuando el jugador encanta a nivel 5000:
     *   ANTES: Mth.clamp(5000, 0, 255) → 255 (el encantamiento queda en 255)
     *   AHORA: devolvemos 5000 directamente (el encantamiento queda en 5000)
     *
     * El campo interno de ItemEnchantments.Mutable es un Object2IntOpenHashMap<Holder<Enchantment>>,
     * que acepta valores int completos — no hay pérdida de datos.
     */
    @Redirect(
            method = "set",
            at = @At(
                    value = "INVOKE",
                    // Target CORRECTO: Mth.clamp(int value, int min, int max)
                    // (no Math.min que es el target incorrecto del mixin anterior)
                    target = "Lnet/minecraft/util/Mth;clamp(III)I"
            )
    )
    private int bypassEnchantmentLevelCap(int value, int min, int max) {
        // Devolver el valor SIN recortar.
        // min=0 y max=255 son los argumentos de Mojang — los ignoramos.
        // El nivel puede ahora llegar hasta Config.MAX_ENCHANTMENT_LEVEL (10,000).
        return value;
    }
}