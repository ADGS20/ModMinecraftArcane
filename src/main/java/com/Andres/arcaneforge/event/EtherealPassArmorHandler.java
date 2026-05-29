package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class EtherealPassArmorHandler {

    private static final ResourceKey<Enchantment> ETHEREAL_WALK_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "ethereal_walk"));

    private static final List<TimedBlock> BLOCKS_TO_CLEAN = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator()) return;

        long gameTime = player.level().getGameTime();
        BlockPos playerFeetPos = player.blockPosition().below();

        // --- 1. LIMPIEZA INTELIGENTE Y REFRESCO (Anti-Caídas) ---
        if (!BLOCKS_TO_CLEAN.isEmpty()) {
            Iterator<TimedBlock> iterator = BLOCKS_TO_CLEAN.iterator();
            while (iterator.hasNext()) {
                TimedBlock timedBlock = iterator.next();

                if (gameTime >= timedBlock.expiryTime) {
                    // Esperanza de vida si el jugador sigue encima
                    if (timedBlock.pos.equals(playerFeetPos) && !player.isCreative()) {
                        timedBlock.expiryTime = gameTime + 40;
                        continue;
                    }

                    if (player.level().getBlockState(timedBlock.pos).is(Blocks.BARRIER)) {
                        player.level().setBlock(timedBlock.pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    iterator.remove();
                }
            }
        }

        if (player.isCreative()) return;

        // --- 2. CHEQUEO DE SEGURIDAD PARA EL AGUA (Karmaland Anti-Bugs) ---
        // 🌊 Si el jugador está nadando o metido en agua, cancelamos para no romper las físicas de nado
        if (player.isSwimming() || player.isInWater()) {
            return;
        }

        // --- 3. CHEQUEO DE ARMADURA MÍSTICA ---
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (leggings.isEmpty() || boots.isEmpty()) return;

        ItemEnchantments leggingEnchants = leggings.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments bootsEnchants = boots.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> walkOpt = registry.get(ETHEREAL_WALK_KEY);

            if (walkOpt.isPresent()) {
                var enchantmentHolder = walkOpt.get();

                if (leggingEnchants.getLevel(enchantmentHolder) > 0 && bootsEnchants.getLevel(enchantmentHolder) > 0) {

                    // Si está en el aire esprintando y cayendo
                    if (!player.onGround() && player.isSprinting() && player.getDeltaMovement().y < 0) {

                        // Requisito de combustible (XP total > 0)
                        if (player.experienceLevel <= 0 && player.experienceProgress <= 0) {
                            return;
                        }

                        BlockPos posBajoPies = player.blockPosition().below();

                        // 🌊 Doble seguridad: Validamos que el bloque de abajo sea AIRE puro, no agua ni corrientes
                        if (player.level().getBlockState(posBajoPies).isAir()) {

                            // Colocamos la barrera física sólida
                            player.level().setBlock(posBajoPies, Blocks.BARRIER.defaultBlockState(), 3);

                            // Gasto de combustible (XP y Durabilidad)
                            player.giveExperiencePoints(-1);

                            if (player instanceof ServerPlayer serverPlayer) {
                                leggings.hurtAndBreak(1, serverPlayer, EquipmentSlot.LEGS);
                                boots.hurtAndBreak(1, serverPlayer, EquipmentSlot.FEET);
                            }

                            // Estabilizamos el vuelo plano y suave
                            player.fallDistance = 0.0f;
                            Vec3 currentMov = player.getDeltaMovement();
                            player.setDeltaMovement(currentMov.x, 0.0, currentMov.z);
                            player.hurtMarked = true;

                            // Espectaculares partículas mágicas moradas
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x9400D3, 1.3f),
                                        player.getX(), player.getY() - 0.05, player.getZ(),
                                        6, 0.15, 0.0, 0.15, 0.0);
                            }

                            // Caduca a los 6 segundos (120 ticks)
                            long expiry = gameTime + 120;

                            boolean yaExiste = false;
                            for (TimedBlock tb : BLOCKS_TO_CLEAN) {
                                if (tb.pos.equals(posBajoPies)) {
                                    yaExiste = true;
                                    break;
                                }
                            }
                            if (!yaExiste) {
                                BLOCKS_TO_CLEAN.add(new TimedBlock(posBajoPies, expiry));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en el Handler del conjunto Paso Etéreo: {}", e.getMessage());
        }
    }

    private static class TimedBlock {
        final BlockPos pos;
        long expiryTime;

        TimedBlock(BlockPos pos, long expiryTime) {
            this.pos = pos;
            this.expiryTime = expiryTime;
        }
    }
}