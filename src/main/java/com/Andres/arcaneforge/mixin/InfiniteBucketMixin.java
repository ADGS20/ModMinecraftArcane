package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * CUBETA INFINITA (infinite_bucket) — si la cubeta que se vacia tiene este
 * encantamiento, se queda igual de llena en vez de convertirse en cubeta
 * vacia. getEmptySuccessItem() es el unico punto donde vanilla decide ese
 * resultado tanto para BucketItem#use() (agua/lava) como para
 * SolidBucketItem#useOn() (nieve en polvo), asi que basta con interceptarlo
 * aqui una sola vez para cubrir ambos casos.
 */
@Mixin(BucketItem.class)
public abstract class InfiniteBucketMixin {

    @Unique
    private static final ResourceKey<Enchantment> AF_INFINITE_BUCKET_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "infinite_bucket"));

    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void arcaneforge$keepBucketFull(ItemStack itemStack, Player player, CallbackInfoReturnable<ItemStack> cir) {
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(AF_INFINITE_BUCKET_KEY);
            if (opt.isEmpty()) return;

            ItemEnchantments enchants = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchants.getLevel(opt.get()) > 0) {
                // ItemUtils.createFilledResult() hace itemStack.consume(1, player) sobre el
                // stack original justo despues de llamar aqui: si devolvemos la MISMA
                // referencia, ese consume() tambien vacia la copia que "devolvimos",
                // dejando la cubeta en 0. Por eso hay que devolver una copia aparte.
                cir.setReturnValue(itemStack.copy());
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.error("[INFINITE-BUCKET] error", e);
        }
    }
}
