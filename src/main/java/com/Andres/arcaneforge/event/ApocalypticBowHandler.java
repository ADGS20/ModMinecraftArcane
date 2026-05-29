package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ApocalypticBowHandler {

    private static final ResourceKey<Enchantment> APOCALYPTIC_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "apocalyptic_judgment"));

    @SubscribeEvent
    public static void onApocalypticImpact(LivingDamageEvent.Pre event) {
        try {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide()) return;

            DamageSource source = event.getSource();
            Entity attacker = source.getEntity();

            // Verificamos que sea un ataque a distancia (proyectil)
            if (attacker instanceof LivingEntity livingAttacker && source.getDirectEntity() != null && !source.getDirectEntity().equals(attacker)) {

                // DETECTOR DUAL: Buscamos un arco O una ballesta en la mano principal
                ItemStack weapon = livingAttacker.getMainHandItem();
                boolean isRangedWeapon = weapon.is(net.minecraft.world.item.Items.BOW) || weapon.is(net.minecraft.world.item.Items.CROSSBOW);

                // Si la mano principal no tiene un arma a distancia válida, miramos la mano secundaria (offhand)
                if (weapon.isEmpty() || !isRangedWeapon) {
                    weapon = livingAttacker.getOffhandItem();
                    isRangedWeapon = weapon.is(net.minecraft.world.item.Items.BOW) || weapon.is(net.minecraft.world.item.Items.CROSSBOW);
                }

                // Si encontramos cualquiera de las dos armas, procesamos la magia arcana
                if (!weapon.isEmpty() && isRangedWeapon) {
                    ItemEnchantments enchants = weapon.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    var registry = livingAttacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantOpt = registry.get(APOCALYPTIC_KEY);

                    if (enchantOpt.isPresent()) {
                        int level = enchants.getLevel(enchantOpt.get());
                        if (level > 0 && livingAttacker.level() instanceof ServerLevel serverLevel) {
                            Vec3 pos = target.position();
                            RandomSource random = serverLevel.getRandom();

                            // Invocar la tormenta de rayos según el nivel
                            for (int i = 0; i < level; i++) {
                                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
                                lightning.setPos(pos.x + (random.nextDouble() - 0.5) * 2, pos.y, pos.z + (random.nextDouble() - 0.5) * 2);
                                serverLevel.addFreshEntity(lightning);
                            }

                            // Explosión de área mística
                            serverLevel.explode(null, pos.x, pos.y, pos.z, 2.0f + (level * 1.0f), false, ServerLevel.ExplosionInteraction.NONE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Apocalyptic Bow Handler error: {}", e.getMessage());
        }
    }
}