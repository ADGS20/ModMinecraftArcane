package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REPARACION ARCANA (arcane_mending)
 *
 * Encantamiento del mod que repara automaticamente los objetos danados
 * consumiendo la EXPERIENCIA QUE TIENE EL JUGADOR (no la de matar mobs).
 *
 * Funciona en cualquier objeto con durabilidad (herramientas, armadura, elitros).
 * Cuanto mas alto el nivel, mas durabilidad recupera por punto de XP.
 *
 * Es server-side puro: no toca codigo de cliente, asi que es seguro en servidor.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneMendingHandler {

    private static final ResourceKey<Enchantment> ARCANE_MENDING_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_mending"));

    // Cada cuantos ticks intentamos reparar (20 ticks = 1 segundo).
    // Reparar cada segundo evita sobrecargar el servidor.
    private static final int REPAIR_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Solo actuamos una vez por segundo.
        if (player.level().getGameTime() % REPAIR_INTERVAL != 0) return;

        // Si el jugador no tiene NADA de experiencia, no hay con que reparar.
        if (serverPlayer.experienceLevel <= 0 && serverPlayer.experienceProgress <= 0f
                && serverPlayer.totalExperience <= 0) {
            return;
        }

        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> mendOpt =
                    registry.get(ARCANE_MENDING_KEY);
            if (mendOpt.isEmpty()) return;
            var mendHolder = mendOpt.get();

            // Reunimos todos los objetos equipados/inventario que tengan el encanto
            // y esten danados.
            List<ItemStack> candidates = collectMendable(serverPlayer, mendHolder);
            if (candidates.isEmpty()) return;

            // Reparamos UN objeto por ciclo (el primero danado) para repartir el XP
            // y no vaciar la experiencia de golpe.
            for (ItemStack stack : candidates) {
                if (stack.getDamageValue() <= 0) continue;

                int level = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .getLevel(mendHolder);
                if (level <= 0) continue;

                // Cada punto de XP repara (2 * nivel) de durabilidad.
                int durabilityPerXp = 2 * level;

                // Cuanta XP podemos gastar este ciclo: hasta 5 puntos, pero sin pasarnos
                // de lo que necesita el objeto ni de lo que tiene el jugador.
                int xpAvailable = getPlayerTotalXpPoints(serverPlayer);
                if (xpAvailable <= 0) return;

                int damage = stack.getDamageValue();
                int xpNeeded = Math.max(1, (int) Math.ceil((double) damage / durabilityPerXp));
                int xpToSpend = Math.min(Math.min(5, xpNeeded), xpAvailable);
                if (xpToSpend <= 0) continue;

                int repaired = xpToSpend * durabilityPerXp;
                stack.setDamageValue(Math.max(0, damage - repaired));

                // Consumimos la XP que TIENE el jugador.
                serverPlayer.giveExperiencePoints(-xpToSpend);

                // Efecto: un pequeno sonido y particulas para que se note la reparacion.
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.6f);
                    // Particula tipo "polvo" magico (mismo patron que ya usa tu mod).
                    serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x55FFFF, 1.0f),
                            serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
                            6, 0.3, 0.5, 0.3, 0.0);
                }

                // Reparamos solo un objeto por ciclo.
                break;
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("Error en Reparacion Arcana: {}", e.getMessage());
        }
    }

    /** Junta los objetos del jugador que tienen arcane_mending y estan danados. */
    private static List<ItemStack> collectMendable(
            ServerPlayer player,
            net.minecraft.core.Holder<Enchantment> mendHolder) {
        List<ItemStack> out = new ArrayList<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) continue;
            int lvl = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                    .getLevel(mendHolder);
            if (lvl > 0) out.add(stack);
        }
        return out;
    }

    /** Aproxima cuantos puntos de XP tiene el jugador en total. */
    private static int getPlayerTotalXpPoints(ServerPlayer player) {
        // totalExperience refleja los puntos acumulados; si fuese 0 pero hay nivel,
        // damos al menos un margen segun el nivel actual.
        int total = player.totalExperience;
        if (total <= 0 && player.experienceLevel > 0) {
            total = player.experienceLevel; // margen conservador
        }
        return Math.max(0, total);
    }
}
