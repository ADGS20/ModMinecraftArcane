package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;
import java.util.Random;

/**
 * GOLPE PARTIDOR — encantamiento de hacha.
 *
 * Al golpear: probabilidad de romper el bloqueo de escudo del objetivo (si
 * estaba bloqueando) y aplicarle un aturdimiento breve (Lentitud + Fatiga al
 * minar). Es "de bandera" como Corte del Vacio / Filo Insaciable: la formula
 * usa el nivel real sin techo en 3, escalando suave hasta el limite real de
 * 255 que permite la Arcane Forge con Pedestal.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class AxeSunderingHandler {

    private static final Random RNG = new Random();

    private static final ResourceKey<Enchantment> GOLPE_PARTIDOR =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "golpe_partidor"));

    @SubscribeEvent
    public static void onAxeHit(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ItemTags.AXES)) return;

        Level level = attacker.level();
        int rawLevel;
        try {
            HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(GOLPE_PARTIDOR);
            if (opt.isEmpty()) return;
            ItemEnchantments enchants = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            rawLevel = enchants.getLevel(opt.get());
        } catch (Exception e) {
            return;
        }
        if (rawLevel <= 0) return;

        try {
            // Probabilidad: ~10% a nivel 1, tope real 80% cerca de nivel 250.
            float chance = Math.min(0.08f + 0.0028f * rawLevel, 0.80f);
            if (RNG.nextFloat() >= chance) return;

            // Duracion del aturdimiento: ~1.15s a nivel 1, tope 5s (100 ticks) cerca de nivel 27+.
            int duration = (int) Math.min(20 + 3 * rawLevel, 100);

            if (target.isBlocking()) {
                ItemStack shield = target.getUseItem();
                target.stopUsingItem();
                if (target instanceof Player targetPlayer && !shield.isEmpty()) {
                    targetPlayer.getCooldowns().addCooldown(shield, duration);
                }
            }

            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 0, false, true, true));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("GolpePartidor error: {}", e.getMessage());
        }
    }
}
