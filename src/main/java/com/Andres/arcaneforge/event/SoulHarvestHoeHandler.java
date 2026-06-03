package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ArcaneForge.MODID)
public class SoulHarvestHoeHandler {

    private static final ResourceKey<Enchantment> SOUL_HARVEST_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "soul_harvest"));

    // 🌾 PARTE A: AGRICULTURA MÍSTICA
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() == null || event.getLevel().isClientSide() || !(event.getBreaker() instanceof ServerPlayer serverPlayer)) return;

        ItemStack tool = serverPlayer.getMainHandItem();
        ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> harvestOpt = registry.get(SOUL_HARVEST_KEY);
            if (harvestOpt.isEmpty() || enchants.getLevel(harvestOpt.get()) <= 0) return;

            BlockPos originPos = event.getPos();
            BlockState originState = event.getState();
            if (!(originState.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(originState)) return;

            int level = enchants.getLevel(harvestOpt.get());

            // Radio: +1 por cada 10 niveles, capado a 6 (13x13) para evitar lag.
            int radius = Math.min(6, 1 + (level / 10));

            // Nivel de Fortuna de la azada: multiplica MUCHO el cultivo principal.
            int fortune = getFortuneLevel(registry, enchants);

            Level world = serverPlayer.level();
            RandomSource random = world.getRandom();

            // Recogemos los drops del bloque central (ya roto por el juego).
            List<ItemStack> collected = new ArrayList<>();
            event.getDrops().forEach(e -> collected.add(e.getItem()));
            event.getDrops().clear();

            // Cosecha en area: rompe cultivos maduros, replanta y junta sus drops.
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos targetPos = originPos.offset(x, 0, z);
                    BlockState targetState = world.getBlockState(targetPos);
                    if (targetState.getBlock() instanceof CropBlock targetCrop && targetCrop.isMaxAge(targetState)
                            && world instanceof ServerLevel serverLevel) {
                        List<ItemStack> drops = Block.getDrops(targetState, serverLevel, targetPos, null, serverPlayer, tool);
                        collected.addAll(drops);
                        world.setBlock(targetPos, targetCrop.getStateForAge(0), 3);
                        tool.hurtAndBreak(1, serverPlayer, serverPlayer.getEquipmentSlotForItem(tool));
                    }
                }
            }

            // Separamos en CULTIVO PRINCIPAL y SEMILLAS, aplicando Fortuna al cultivo.
            List<ItemStack> cultivos = new ArrayList<>();
            List<ItemStack> semillas = new ArrayList<>();
            for (ItemStack drop : collected) {
                if (drop.isEmpty()) continue;
                if (esSemilla(drop)) {
                    semillas.add(drop);
                } else {
                    // Fortuna multiplica el cultivo principal. Con Fortuna alta
                    // (ej. 255) sale muchisimo: cada nivel suma un bonus aleatorio.
                    if (fortune > 0) {
                        int bonus = 1 + random.nextInt(fortune + 1); // x1 .. x(fortune+1)
                        drop.setCount(drop.getCount() * bonus);
                    }
                    cultivos.add(drop);
                }
            }

            ArcaneForgeBlockEntity forge = findHarvestForge(serverPlayer);

            // CULTIVO PRINCIPAL: primero al inventario; si se llena, al cofre; si no, al suelo.
            for (ItemStack c : cultivos) {
                ItemStack left = giveToPlayerThenChest(serverPlayer, c, forge);
                if (!left.isEmpty()) Block.popResource(world, originPos, left);
            }
            // SEMILLAS: directo al cofre; si no hay o no cabe, al inventario; si no, al suelo.
            for (ItemStack s : semillas) {
                ItemStack left = (forge != null) ? forge.insertIntoLinkedChests(s) : s;
                if (!left.isEmpty() && !serverPlayer.getInventory().add(left)) {
                    Block.popResource(world, originPos, left);
                }
            }

            // Replantar tambien el bloque central.
            world.setBlock(originPos, crop.getStateForAge(0), 3);

        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en agricultura de Cosecha de Almas: {}", e.getMessage());
        }
    }

    /** Mete primero en el inventario; lo que no quepa va al cofre marcado. Devuelve lo que sobre. */
    private static ItemStack giveToPlayerThenChest(ServerPlayer player, ItemStack stack, ArcaneForgeBlockEntity forge) {
        ItemStack work = stack.copy();
        // El inventario admite stacks completos; add() reduce el conteo de 'work'.
        player.getInventory().add(work);
        if (work.isEmpty()) return ItemStack.EMPTY;
        // Lo que no cupo en el inventario va al cofre.
        if (forge != null) {
            return forge.insertIntoLinkedChests(work);
        }
        return work;
    }

    /** True si el item es una semilla (para replantar), no el producto de la cosecha. */
    private static boolean esSemilla(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD)) {
            return true;
        }
        // Por si hay semillas de otros mods: su id suele contener "seed".
        try {
            Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null && id.getPath().contains("seed");
        } catch (Exception e) {
            return false;
        }
    }

    private static int getFortuneLevel(net.minecraft.core.Registry<Enchantment> registry, ItemEnchantments enchants) {
        try {
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> opt = registry.get(Enchantments.FORTUNE);
            return opt.map(enchants::getLevel).orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Mesa Arcane Forge marcada por el jugador con la varita (shift+clic), o null. */
    private static ArcaneForgeBlockEntity findHarvestForge(ServerPlayer player) {
        try {
            var pdata = player.getPersistentData();
            int x = pdata.getInt("arcaneforge_harvest_x").orElse(Integer.MIN_VALUE);
            int y = pdata.getInt("arcaneforge_harvest_y").orElse(Integer.MIN_VALUE);
            int z = pdata.getInt("arcaneforge_harvest_z").orElse(Integer.MIN_VALUE);
            if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) return null;
            BlockPos pos = new BlockPos(x, y, z);
            if (!player.level().isLoaded(pos)) return null;
            BlockEntity be = player.level().getBlockEntity(pos);
            return (be instanceof ArcaneForgeBlockEntity forge) ? forge : null;
        } catch (Exception e) {
            return null;
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
