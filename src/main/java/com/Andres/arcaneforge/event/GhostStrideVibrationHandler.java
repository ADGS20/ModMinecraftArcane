package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.GhostStrideUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;

/**
 * GhostStrideEmissionMixin ya silencia el GameEvent.STEP de cada paso via
 * Player.getMovementEmission(), pero aterrizar tras un salto dispara su
 * propia vibracion (GameEvent.HIT_GROUND) directamente desde
 * Entity.checkFallDamage(), sin pasar por ahi — por eso un Warden que ya
 * tiene al jugador como objetivo lo seguia reubicando cada vez que saltaba
 * aunque llevara Zancada Fantasma puesta.
 *
 * VanillaGameEvent es el punto donde CUALQUIER vibracion vanilla termina
 * pasando justo antes de despacharse (ServerLevel.gameEvent), asi que
 * cancelarla aqui cubre pasos, saltos y nado de una sola vez sin depender
 * de cada sitio de codigo que la dispara por separado.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class GhostStrideVibrationHandler {

    @SubscribeEvent
    public static void onVanillaGameEvent(VanillaGameEvent event) {
        Holder<GameEvent> type = event.getVanillaEvent();
        if (!type.is(GameEvent.STEP.key()) && !type.is(GameEvent.HIT_GROUND.key())
                && !type.is(GameEvent.SWIM.key()) && !type.is(GameEvent.SPLASH.key())) {
            return;
        }

        Entity cause = event.getCause();
        if (cause instanceof LivingEntity living && GhostStrideUtil.isActive(living)) {
            event.setCanceled(true);
        }
    }
}
