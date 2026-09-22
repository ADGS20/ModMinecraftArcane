package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// PowderSnowBlock.canEntityWalkOnPowderSnow(Entity) es el unico punto de
// entrada que vainilla usa tanto para la colision (no hundirse) como para
// caminar por encima de la nieve en polvo; por defecto solo lo permite si
// las botas puestas son de cuero (Item.canWalkOnPowderedSnow -> stack.is
// (LEATHER_BOOTS)). Interceptamos ANTES de esa comprobacion: si las botas
// llevan nuestro encantamiento "paso_nevado", devolvemos true de una vez,
// sin tocar el comportamiento normal para el resto de items/botas.
@Mixin(PowderSnowBlock.class)
public abstract class SnowWalkMixin {

    private static final ResourceKey<Enchantment> PASO_NEVADO_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "paso_nevado"));

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void arcaneforge$snowWalk(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof LivingEntity living)) return;
        // A diferencia del resto de los handlers del mod, esto es pura forma
        // de colision fisica (no logica de servidor): debe evaluarse igual
        // en cliente y servidor, si no el jugador se hunde visualmente en el
        // cliente mientras el servidor lo mantiene arriba (desync/tembleque).

        ItemStack boots = living.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        try {
            var registry = living.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(PASO_NEVADO_KEY);
            if (opt.isEmpty()) return;

            ItemEnchantments enchants = boots.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchants.getLevel(opt.get()) > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        } catch (Exception ignored) {
        }
    }
}
