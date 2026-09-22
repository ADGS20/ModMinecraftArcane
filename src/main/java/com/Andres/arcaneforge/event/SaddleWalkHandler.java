package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * SILLA ARCANA — reutiliza la silla (minecraft:saddle) de siempre, pero
 * encantada en la Forja: no hace falta un item nuevo (la Forja ya encanta
 * cualquier cosa sin filtrar por supported_items), y asi cualquier montura
 * con slot de silla (caballo, cerdo, llama, y de regalo camello/strider)
 * queda cubierta por igual, sin discriminar a ninguna — mismo mecanismo
 * generico Mob.isSaddled()/EquipmentSlot.SADDLE para todas.
 *
 *  - PASO ACUATICO: la montura camina sobre el agua sin hundirse (mismo
 *    truco que Paso Infernal con la lava en LavaWalkHandler).
 *  - CAMINATA ETEREA: si la silla lleva ese encantamiento y el jinete es un
 *    jugador, la montura crea plataformas invisibles al caer en el aire
 *    (igual que EtherealPassArmorHandler para el jugador a pie), pero el
 *    combustible de XP sale del JINETE al TRIPLE de coste (3 puntos por
 *    activacion en vez de 1), porque cargar con una montura entera es mucho
 *    mas exigente que solo llevar al jugador.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class SaddleWalkHandler {

    private static final ResourceKey<Enchantment> PASO_ACUATICO_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "paso_acuatico"));
    private static final ResourceKey<Enchantment> ETHEREAL_WALK_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "ethereal_walk"));

    private static final List<TimedBlock> BLOCKS_TO_CLEAN = new ArrayList<>();

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
    public static void onMountTick(EntityTickEvent.Post event) {
        try {
            if (!(event.getEntity() instanceof Mob mob)) return;
            if (mob.level().isClientSide()) return;
            if (!mob.isSaddled()) return;

            ItemStack saddle = mob.getItemBySlot(EquipmentSlot.SADDLE);

            cleanupExpiredBlocks(mob);
            handleWaterWalk(mob, saddle);
            handleEtherealWalk(mob, saddle);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("SillaArcana error: {}", e.getMessage());
        }
    }

    private static void handleWaterWalk(Mob mob, ItemStack saddle) {
        if (lvl(mob.level(), saddle, PASO_ACUATICO_KEY) <= 0) return;
        if (!mob.isInWater()) return;

        BlockPos check = mob.blockPosition();
        for (int i = 0; i < 3 && !mob.level().getFluidState(check).is(FluidTags.WATER); i++) {
            check = check.below();
        }
        double surfaceY = check.getY() + 1.0;

        if (mob.getY() < surfaceY) {
            mob.setPos(mob.getX(), surfaceY, mob.getZ());
            Vec3 mov = mob.getDeltaMovement();
            mob.setDeltaMovement(mov.x, Math.max(mov.y, 0.0), mov.z);
            mob.fallDistance = 0.0f;
        }
    }

    private static void handleEtherealWalk(Mob mob, ItemStack saddle) {
        if (lvl(mob.level(), saddle, ETHEREAL_WALK_KEY) <= 0) return;
        if (!(mob.getFirstPassenger() instanceof Player rider)) return;
        if (mob.onGround() || mob.getDeltaMovement().y >= 0) return;

        if (rider.experienceLevel <= 0 && rider.experienceProgress <= 0) return;

        BlockPos below = mob.blockPosition().below();
        if (!mob.level().getBlockState(below).isAir()) return;

        mob.level().setBlock(below, Blocks.BARRIER.defaultBlockState(), 3);

        rider.giveExperiencePoints(-3);
        if (rider instanceof ServerPlayer serverPlayer) {
            saddle.hurtAndBreak(1, serverPlayer, EquipmentSlot.SADDLE);
        }

        mob.fallDistance = 0.0f;
        Vec3 mov = mob.getDeltaMovement();
        mob.setDeltaMovement(mov.x, 0.0, mov.z);
        mob.hurtMarked = true;

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(0x9400D3, 1.3f),
                    mob.getX(), mob.getY() - 0.05, mob.getZ(), 8, 0.2, 0.0, 0.2, 0.0);
        }

        long expiry = mob.level().getGameTime() + 120;
        for (TimedBlock tb : BLOCKS_TO_CLEAN) {
            if (tb.pos.equals(below)) return;
        }
        BLOCKS_TO_CLEAN.add(new TimedBlock(below, expiry));
    }

    private static void cleanupExpiredBlocks(Mob mob) {
        if (BLOCKS_TO_CLEAN.isEmpty()) return;
        long gameTime = mob.level().getGameTime();
        BlockPos feetPos = mob.blockPosition().below();

        Iterator<TimedBlock> iterator = BLOCKS_TO_CLEAN.iterator();
        while (iterator.hasNext()) {
            TimedBlock timedBlock = iterator.next();
            if (gameTime < timedBlock.expiryTime) continue;

            if (timedBlock.pos.equals(feetPos)) {
                timedBlock.expiryTime = gameTime + 40;
                continue;
            }

            if (mob.level().getBlockState(timedBlock.pos).is(Blocks.BARRIER)) {
                mob.level().setBlock(timedBlock.pos, Blocks.AIR.defaultBlockState(), 3);
            }
            iterator.remove();
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
