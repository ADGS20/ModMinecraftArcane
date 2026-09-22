package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * PASO INFERNAL — con este encantamiento en las botas, el jugador flota
 * justo encima de la superficie de la lava en vez de hundirse (como
 * caminar sobre agua), y no se quema ni recibe daño mientras lo lleva
 * puesto. No modifica bloques del mundo (a diferencia de Paso Helado, que
 * congela el agua): es un ajuste de física + inmunidad al daño, nada más.
 *
 * El nivel en si es binario (activa/no activa la habilidad), pero desde la
 * pasada "reward 255" tambien concede Velocidad mientras flota sobre lava,
 * escalando con el nivel — asi invertir mas en la Forja sigue dando algo
 * (ver ArcaneForgeBlockEntity.getRealMaxLevel "paso_infernal" = 60).
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class LavaWalkHandler {

    private static final ResourceKey<Enchantment> PASO_INFERNAL_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "paso_infernal"));

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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        try {
            Player player = event.getEntity();
            if (player.level().isClientSide() || player.isSpectator()) return;

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (boots.isEmpty()) return;
            int level = lvl(player.level(), boots, PASO_INFERNAL_KEY);
            if (level <= 0) return;

            boolean inLava = player.isInLava();
            if (inLava || player.isOnFire()) {
                player.clearFire();
            }
            if (!inLava) return;

            // Bono de Velocidad mientras flota, escalando con el nivel (amplificador
            // +1 cada 20 niveles, hasta III a nivel 60).
            int speedAmplifier = Math.min(level / 20, 3);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, 40, speedAmplifier, false, false, true));

            // Buscamos el primer bloque de lava justo bajo (o en) los pies y
            // flotamos justo encima, en vez de dejar que el jugador siga
            // hundiendose como si no llevara nada puesto.
            BlockPos check = player.blockPosition();
            for (int i = 0; i < 2 && !player.level().getFluidState(check).is(FluidTags.LAVA); i++) {
                check = check.below();
            }
            double surfaceY = check.getY() + 1.0;

            if (player.getY() < surfaceY) {
                player.setPos(player.getX(), surfaceY, player.getZ());
                Vec3 mov = player.getDeltaMovement();
                player.setDeltaMovement(mov.x, Math.max(mov.y, 0.0), mov.z);
                player.fallDistance = 0.0f;
                player.hurtMarked = true;
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("PasoInfernal error: {}", e.getMessage());
        }
    }

    /** Segundo seguro: cancela de raiz el daño de lava/fuego mientras las botas esten puestas. */
    @SubscribeEvent
    public static void onLavaDamage(LivingDamageEvent.Pre event) {
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            if (player.level().isClientSide()) return;

            String msgId = event.getSource().type().msgId();
            if (!msgId.equals("lava") && !msgId.equals("inFire") && !msgId.equals("onFire")) return;

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (boots.isEmpty()) return;
            if (lvl(player.level(), boots, PASO_INFERNAL_KEY) > 0) {
                event.setNewDamage(0f);
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("PasoInfernalDamage error: {}", e.getMessage());
        }
    }
}
