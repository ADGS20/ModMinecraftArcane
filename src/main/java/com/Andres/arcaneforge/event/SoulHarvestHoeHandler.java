package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent; // Usamos el evento ultraestable que ya te funciona

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class SoulHarvestHoeHandler {

    private static final ResourceKey<Enchantment> SOUL_HARVEST_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "soul_harvest"));

    // 🌾 PARTE A: AGRICULTURA MÍSTICA (Área por cada 10 niveles, Replantado e Inventario directo)
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() == null || event.getLevel().isClientSide() || !(event.getBreaker() instanceof ServerPlayer serverPlayer)) return;

        ItemStack tool = serverPlayer.getMainHandItem();
        ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> harvestOpt = registry.get(SOUL_HARVEST_KEY);

            if (harvestOpt.isPresent() && enchants.getLevel(harvestOpt.get()) > 0) {
                BlockPos originPos = event.getPos();
                BlockState originState = event.getState();

                // Comprobamos si el bloque original roto es un cultivo maduro
                if (originState.getBlock() instanceof CropBlock crop && crop.isMaxAge(originState)) {
                    int level = enchants.getLevel(harvestOpt.get());

                    // ⚖️ EQUILIBRIO CONTRA LAG (Tu genial sugerencia):
                    // +1 bloque de radio por cada 10 niveles del encantamiento.
                    // Capado a un máximo estricto de radio 6 (13x13 bloques) para proteger el procesador de explosiones de lag.
                    int radius = 1 + (level / 10);
                    if (radius > 6) {
                        radius = 6;
                    }

                    Level world = serverPlayer.level();

                    // Vaciamos la lista de drops del evento vanilla para recolectarlos nosotros directo al inventario
                    List<ItemStack> collectedDrops = new ArrayList<>();
                    event.getDrops().forEach(itemEntity -> collectedDrops.add(itemEntity.getItem()));
                    event.getDrops().clear();

                    // Procesamos el área de efecto mística alrededor del bloque roto
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            // Saltamos el origen porque ya fue destruido por el juego base
                            if (x == 0 && z == 0) continue;

                            BlockPos targetPos = originPos.offset(x, 0, z);
                            BlockState targetState = world.getBlockState(targetPos);

                            if (targetState.getBlock() instanceof CropBlock targetCrop && targetCrop.isMaxAge(targetState)) {
                                if (world instanceof ServerLevel serverLevel) {
                                    // 1. Obtener los drops del cultivo secundario maduro
                                    List<ItemStack> drops = Block.getDrops(targetState, serverLevel, targetPos, null, serverPlayer, tool);
                                    collectedDrops.addAll(drops);

                                    // 2. Romper místicamente y auto-replantar (Edad de cultivo a 0)
                                    world.setBlock(targetPos, targetCrop.getStateForAge(0), 3);

                                    // Desgaste progresivo de la herramienta
                                    tool.hurtAndBreak(1, serverPlayer, serverPlayer.getEquipmentSlotForItem(tool));
                                }
                            }
                        }
                    }

                    // 📦 ENVÍO DIRECTO AL INVENTARIO
                    for (ItemStack drop : collectedDrops) {
                        if (!serverPlayer.getInventory().add(drop)) {
                            // Fallback de seguridad: si el inventario se llena, lo tira al suelo
                            Block.popResource(world, originPos, drop);
                        }
                    }

                    // Forzamos el replantado automático también en el bloque central de origen
                    world.setBlock(originPos, crop.getStateForAge(0), 3);
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en agricultura de Cosecha de Almas: {}", e.getMessage());
        }
    }

    // ⚔️ PARTE B: GUADAÑA DE ALMAS (Absorber experiencia al golpear mobs)
    @SubscribeEvent
    public static void onHitEntity(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof net.minecraft.world.item.HoeItem)) return;

        ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> harvestOpt = registry.get(SOUL_HARVEST_KEY);

            if (harvestOpt.isPresent() && enchants.getLevel(harvestOpt.get()) > 0) {
                int level = enchants.getLevel(harvestOpt.get());

                float chance = Math.min(0.85f, level * 0.15f);
                if (player.getRandom().nextFloat() < chance) {
                    player.giveExperiencePoints(3 + (level / 2));

                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x4A0E4E, 1.0f),
                                event.getEntity().getX(), event.getEntity().getY() + 1.0, event.getEntity().getZ(),
                                10, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en guadaña de Cosecha de Almas: {}", e.getMessage());
        }
    }
}