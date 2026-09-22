package com.Andres.arcaneforge.item;

import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.moon.ArcaneMoonData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fragmento Lunar — consumible que adelanta la proxima Luna Oscura/Roja
 * (ver moon/ArcaneMoonData). Tiene su propio cooldown independiente del
 * ciclo de la Luna para que no se pueda encadenar y saltar varias rondas
 * de golpe consumiendo varios seguidos.
 */
public class LunarFragmentItem extends Item {

    public LunarFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ArcaneMoonData data = ArcaneMoonData.get(serverLevel);
        long gameTime = serverLevel.getGameTime();
        long cooldownTicks = Config.MOON_ACCELERATOR_COOLDOWN_DAYS * 24000L;

        if (data.getLastAcceleratorUseGameTime() >= 0
                && gameTime - data.getLastAcceleratorUseGameTime() < cooldownTicks) {
            player.sendSystemMessage(Component.literal(
                    "El Fragmento Lunar todavia esta enfriandose. Espera un poco mas."));
            return InteractionResult.FAIL;
        }

        long daysSkippedTicks = Config.MOON_ACCELERATOR_DAYS_SKIPPED * 24000L;
        data.useAccelerator(gameTime, daysSkippedTicks);

        player.sendSystemMessage(Component.literal(
                "§5El Fragmento Lunar vibra... la proxima Luna Oscura se acerca."));
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS, 1.0f, 1.4f);

        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
