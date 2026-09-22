package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

/**
 * VAMPIRO — al golpear con una espada encantada, roba un porcentaje del
 * daño hecho como vida propia. Si el atacante ya está a vida llena cuando
 * le tocaría curarse, esa curación desperdiciada se convierte en vida
 * MÁXIMA extra permanente (un "corazón extra" real, vía AttributeModifier
 * sobre Attributes.MAX_HEALTH), y el jugador aparece con ese corazón ya
 * lleno. Sin techo: cada golpe a vida llena puede seguir sumando más.
 *
 * El corazón extra actúa como un escudo: el siguiente daño que reciba el
 * jugador se resta primero de ahí (bajando su vida máxima) antes de tocar
 * su vida real — como la Absorción vainilla, pero sin decaer con el
 * tiempo. Solo se gasta si te golpean.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class VampireHandler {

    private static final ResourceKey<Enchantment> VAMPIRO_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "vampiro"));

    /** Id fijo del AttributeModifier que representa el/los corazon(es) extra acumulados. */
    private static final Identifier BONUS_HEART_ID =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "vampiro_bonus_heart");

    private static int lvl(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(key);
            if (opt.isEmpty()) return 0;
            ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get());
        } catch (Exception e) { return 0; }
    }

    @SubscribeEvent
    public static void onVampiroLifesteal(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            Entity attackerEntity = event.getSource().getEntity();
            if (!(attackerEntity instanceof LivingEntity livingAttacker)) return;

            int level;
            if (livingAttacker instanceof AbstractGolem golem && ArcaneGolemUtil.isArcaneGolem(golem)) {
                // El golem no lleva espada: su nivel de Vampiro es el que se le
                // vinculo con el cetro, sin gate de arma.
                level = ArcaneGolemUtil.golemEnchantLevel(golem, VAMPIRO_KEY);
            } else {
                ItemStack weapon = livingAttacker.getMainHandItem();
                if (!weapon.is(ItemTags.SWORDS)) return;
                level = lvl(livingAttacker.level(), weapon, VAMPIRO_KEY);
            }
            if (level <= 0) return;

            // Techo re-escalado: a nivel 3 sigue robando 15% (igual que antes),
            // pero ahora el % de robo de vida sigue subiendo hasta 50%.
            float stolen = event.getOriginalDamage() * Math.min(0.05f * level, 0.50f);
            if (stolen <= 0) return;

            if (livingAttacker.getHealth() < livingAttacker.getMaxHealth()) {
                livingAttacker.heal(Math.min(stolen, livingAttacker.getMaxHealth() - livingAttacker.getHealth()));
                return;
            }

            // Vida llena: solo los jugadores acumulan corazon extra permanente.
            if (!(livingAttacker instanceof ServerPlayer player)) return;

            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            float gain = stolen * Math.min(0.3f + 0.15f * level, 3.0f);
            AttributeModifier existing = maxHealthAttr.getModifier(BONUS_HEART_ID);
            double newBonus = (existing != null ? existing.amount() : 0.0) + gain;

            maxHealthAttr.addOrReplacePermanentModifier(
                    new AttributeModifier(BONUS_HEART_ID, newBonus, AttributeModifier.Operation.ADD_VALUE));
            player.setHealth((float) Math.min(player.getHealth() + gain, player.getMaxHealth()));

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new DustParticleOptions(0xB00020, 1.2f),
                        player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Vampiro error: {}", e.getMessage());
        }
    }

    /** El corazon extra absorbe el proximo golpe antes que la vida real, como un escudo. */
    @SubscribeEvent
    public static void onBonusHeartShield(LivingDamageEvent.Pre event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (player.level().isClientSide()) return;

            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr == null) return;

            AttributeModifier existing = maxHealthAttr.getModifier(BONUS_HEART_ID);
            if (existing == null || existing.amount() <= 0) return;

            double bonus = existing.amount();
            float incoming = event.getNewDamage();
            if (incoming <= 0) return;

            double absorbed = Math.min(bonus, incoming);
            double remainingBonus = bonus - absorbed;

            if (remainingBonus > 0) {
                maxHealthAttr.addOrReplacePermanentModifier(
                        new AttributeModifier(BONUS_HEART_ID, remainingBonus, AttributeModifier.Operation.ADD_VALUE));
            } else {
                maxHealthAttr.removeModifier(BONUS_HEART_ID);
            }
            player.setHealth((float) Math.min(player.getHealth(), player.getMaxHealth()));

            event.setNewDamage((float) (incoming - absorbed));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("VampiroShield error: {}", e.getMessage());
        }
    }
}
