package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ApocalypticBowHandler {

    private static final ResourceKey<Enchantment> APOCALYPTIC_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "apocalyptic_judgment"));

    // ── Efectos al impacto ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onApocalypticImpact(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            DamageSource source = event.getSource();
            Entity attacker = source.getEntity();

            if (!(attacker instanceof LivingEntity livingAttacker)) return;
            if (source.getDirectEntity() == null || source.getDirectEntity().equals(attacker)) return;

            ItemStack weapon = getBowOrCrossbow(livingAttacker);
            if (weapon.isEmpty()) return;

            ItemEnchantments enchants = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            var registry = livingAttacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantOpt = registry.get(APOCALYPTIC_KEY);

            if (enchantOpt.isEmpty()) return;
            int level = enchants.getLevel(enchantOpt.get());
            if (level <= 0) return;
            if (!(livingAttacker.level() instanceof ServerLevel serverLevel)) return;

            Vec3 pos = target.position();
            RandomSource random = serverLevel.getRandom();

            // Rayos: según el nivel del encantamiento
            int boltCount = level;
            for (int i = 0; i < boltCount; i++) {
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                bolt.setPos(
                        pos.x + (random.nextDouble() - 0.5) * 3,
                        pos.y,
                        pos.z + (random.nextDouble() - 0.5) * 3);
                serverLevel.addFreshEntity(bolt);
            }

            // Explosión: crecimiento logarítmico según el nivel
            float explosionRadius = 2.0f + (float) Math.log10(level + 1) * 2.5f;
            serverLevel.explode(null, pos.x, pos.y, pos.z, explosionRadius,
                    false, ServerLevel.ExplosionInteraction.NONE);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ApocalypticBowHandler impact error: {}", e.getMessage());
        }
    }

    // ── Protección de durabilidad ────────────────────────────────────────────

    /**
     * Cancela el consumo de durabilidad del arco si tiene Apocalyptic Judgment.
     * Sin esto, el arco con nivel 1000 se rompe por las interacciones de explosión.
     */
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        try {
            ItemStack bow = event.getBow();
            if (bow.isEmpty()) return;
            if (!bow.is(Items.BOW) && !bow.is(Items.CROSSBOW)) return;

            ItemEnchantments enchants = bow.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            var registry = event.getEntity().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantOpt = registry.get(APOCALYPTIC_KEY);

            if (enchantOpt.isPresent() && enchants.getLevel(enchantOpt.get()) > 0) {
                // Garantizamos que la durabilidad nunca baje de 1
                if (bow.getDamageValue() > 0) {
                    bow.setDamageValue(0);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("ApocalypticBowHandler durability error: {}", e.getMessage());
        }
    }

    // ── Utilidad ────────────────────────────────────────────────────────────

    private static ItemStack getBowOrCrossbow(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        if (main.is(Items.BOW) || main.is(Items.CROSSBOW)) return main;
        ItemStack off = entity.getOffhandItem();
        if (off.is(Items.BOW) || off.is(Items.CROSSBOW)) return off;
        return ItemStack.EMPTY;
    }
}
