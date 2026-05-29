package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneTotemKey {

    // Registramos la clave de nuestro encantamiento exclusivo para buscarlo en el ítem
    private static final ResourceKey<Enchantment> VOID_PROTECTION_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "void_protection"));

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        try {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide() || !(entity instanceof Player player)) return;

            float damage = event.getNewDamage();
            if (player.getHealth() - damage <= 0) {

                ItemStack totemStack = player.getMainHandItem();
                if (!totemStack.is(Items.TOTEM_OF_UNDYING)) {
                    totemStack = player.getOffhandItem();
                }

                if (totemStack.is(Items.TOTEM_OF_UNDYING)) {
                    // VERIFICACIÓN: Consultamos si el Tótem tiene nuestro encantamiento exclusivo
                    ItemEnchantments enchants = totemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    Optional<net.minecraft.core.Holder.Reference<Enchantment>> enchantOpt = registry.get(VOID_PROTECTION_KEY);

                    if (enchantOpt.isPresent() && enchants.getLevel(enchantOpt.get()) > 0) {
                        CustomData customData = totemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                        CompoundTag tag = customData.copyTag();

                        // Si es la primera vez que se activa, le asignamos las 3 cargas
                        if (!tag.contains("TotemCharges")) {
                            tag.putInt("TotemCharges", 3);
                        }

                        int charges = tag.getInt("TotemCharges").orElse(0);

                        if (charges > 0) {
                            event.setNewDamage(0.0f);

                            player.setHealth(4.0f);
                            player.removeAllEffects();

                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 2));
                            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 4));
                            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));

                            double range = 5.0;
                            if (charges == 3) range = 20.0;
                            else if (charges == 2) range = 10.0;

                            AABB area = player.getBoundingBox().inflate(range, range, range);
                            for (Entity target : player.level().getEntities(player, area)) {
                                if (target instanceof LivingEntity livingTarget) {
                                    Vec3 direction = livingTarget.position().subtract(player.position()).normalize();
                                    livingTarget.setDeltaMovement(direction.x * 2.5, 1.2, direction.z * 2.5);
                                    livingTarget.hurtMarked = true;
                                }
                            }

                            charges--;
                            if (charges <= 0) {
                                totemStack.shrink(1);
                                player.sendSystemMessage(Component.literal("§cEl Tótem del Vacío se ha desintegrado por completo."));
                            } else {
                                tag.putInt("TotemCharges", charges);
                                totemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                                player.sendSystemMessage(Component.literal("§d¡Onda expansiva desatada! Te quedan §e" + charges + " §dusos del Tótem."));
                            }

                            if (player.level() instanceof ServerLevel serverLevel) {
                                serverLevel.broadcastEntityEvent(player, (byte) 35);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en el Tótem Prohibido: {}", e.getMessage());
        }
    }
}