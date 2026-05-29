package com.Andres.arcaneforge.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentDisplayMixin {

    @Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
    private static void onGetFullname(Holder<Enchantment> enchantment, int level, CallbackInfoReturnable<Component> cir) {
        MutableComponent nameComponent = enchantment.value().description().copy();

        if (enchantment.is(EnchantmentTags.CURSE)) {
            nameComponent.withStyle(ChatFormatting.RED);
        } else {
            nameComponent.withStyle(ChatFormatting.GRAY);
        }

        if (level != 1 || enchantment.value().getMaxLevel() != 1) {
            if (level <= 10) {
                nameComponent.append(" ").append(Component.translatable("enchantment.level." + level));
            } else {
                nameComponent.append(" ").append(Component.literal(String.valueOf(level)));
            }
        }

        cir.setReturnValue(nameComponent);
    }
}