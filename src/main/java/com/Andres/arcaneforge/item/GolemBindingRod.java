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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Cetro Arcano del Golem — se encanta en la Forja como cualquier otro item
 * (es un item "universal" para el filtro de la Forja: vainilla no lo
 * reconoce en ninguna categoria de encantamiento, asi que se ve la lista
 * completa por defecto), y con clic derecho sobre un golem DE HIERRO le
 * transfiere TODOS los encantamientos que llevaba puestos en ese momento.
 * El cetro se rompe en el proceso (se consume entero, no pierde durabilidad
 * a medias).
 *
 * Solo funciona sobre IronGolem — para SnowGolem existe un item hermano,
 * SnowGolemBindingRod, con su propia textura/nombre y sus propios bonos
 * (el golem de nieve no tiene ATTACK_DAMAGE, es una unidad de rango). Ambos
 * comparten la misma logica de guardado/lectura en ArcaneGolemUtil.
 *
 * Reutiliza la textura de la Vara de Vinculacion (misma identidad visual,
 * sin arte nuevo) pero es una clase e item registrado aparte: esa otra vara
 * tiene su propia logica de vincular cofres a la Forja y no tiene nada que
 * ver con esto.
 */
public class GolemBindingRod extends Item {

    public GolemBindingRod(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(target instanceof IronGolem golem)) return InteractionResult.PASS;

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

        golem.setCustomName(Component.literal("Golem Arcano"));
        golem.setCustomNameVisible(true);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(0x9400D3, 1.5f),
                    golem.getX(), golem.getY() + 1.0, golem.getZ(), 24, 0.4, 0.6, 0.4, 0.0);
        }

        return InteractionResult.CONSUME;
    }
}
