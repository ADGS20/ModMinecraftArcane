package com.Andres.arcaneforge.item;

import com.Andres.arcaneforge.util.ArcaneGolemUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Cetro Arcano del Golem de Nieve — hermano del GolemBindingRod (misma
 * mecanica: se encanta en la Forja, clic derecho transfiere TODOS sus
 * encantamientos y se rompe en el proceso), pero solo actua sobre SnowGolem,
 * con su propia textura (recoloreada a tonos de hielo, sin arte nuevo) y su
 * propio set de bonos.
 *
 * El Golem de Nieve no tiene ATTACK_DAMAGE registrado (vainilla lo hace
 * puramente de rango, ataca con RangedAttackGoal/bolas de nieve), asi que
 * aqui escalamos vida (base 4, mucho mas fragil que el de hierro), velocidad
 * de movimiento y TAMAÑO (mismo tope de 1.5x que el Golem de Hierro — ver
 * applyGolemBonuses). El daño real de sus bolas de nieve — vainilla les pone
 * 0 contra casi todo — lo maneja ArcaneSnowGolemHandler escalando con
 * ArcaneGolemUtil.totalBoundLevels, igual que el daño cuerpo a cuerpo del
 * Golem de Hierro. Con Lealtad vinculada tambien sigue a su dueño como un
 * perro (ver ArcaneGolemHandler.onGolemFollowOwner), compartido con el de
 * hierro.
 */
public class SnowGolemBindingRod extends Item {

    public SnowGolemBindingRod(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(target instanceof SnowGolem golem)) return InteractionResult.PASS;

        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) {
            player.sendSystemMessage(Component.literal("El cetro no tiene ningun encantamiento que transferir. Encantalo en la Forja Arcana primero."));
            return InteractionResult.CONSUME;
        }

        ArcaneGolemUtil.bindWandEnchantsToGolem(golem, stack);
        ArcaneGolemUtil.setOwner(golem, player.getUUID());
        ArcaneGolemUtil.applyGolemBonuses(golem);

        player.setItemInHand(hand, ItemStack.EMPTY);
        player.level().playSound(null, golem.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);

        golem.setCustomName(Component.literal("Golem de Nieve Arcano"));
        golem.setCustomNameVisible(true);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(0x66CCFF, 1.5f),
                    golem.getX(), golem.getY() + 1.0, golem.getZ(), 24, 0.4, 0.6, 0.4, 0.0);
        }

        return InteractionResult.CONSUME;
    }
}
