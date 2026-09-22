package com.Andres.arcaneforge.mixin;

import com.Andres.arcaneforge.event.ArcaneMinerTrades;
import com.Andres.arcaneforge.event.ArcaneVillagerTrades;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * El aldeano "verde" (Nitwit) no tiene profesion real, asi que vanilla nunca
 * le asigna ofertas de trade y por eso nunca abre el GUI de comercio. Justo
 * antes de que la logica vanilla revise si tiene ofertas, si detectamos que
 * es un Nitwit y aun no tiene ofertas, le sorteamos un rol (Arcano o Minero,
 * 50/50) y le inyectamos las ofertas de ese rol.
 *
 * El rol no se guarda aparte: se reconoce mirando las propias ofertas del
 * aldeano (que vanilla ya persiste solo) — si su primer costo es carne, es
 * Minero; si no, es Arcano. Asi evitamos inventar un sistema de guardado
 * propio para un solo flag que ya esta implicito en los datos existentes.
 *
 * Ambos roles tienen un nombre visible con su "nivel de confianza" (propio
 * del mod, nada que ver con el nivel vanilla Novato/Aprendiz), para que el
 * jugador vea de un vistazo cuanto le falta para desbloquear todo. El
 * Arcano tiene 2 ramas de 3 tiers + Nivel 4 combinado (ArcaneVillagerTrades);
 * el Minero tiene una sola escalera de 7 tramos (ArcaneMinerTrades).
 */
@Mixin(Villager.class)
public abstract class ArcaneNitwitTradeMixin {

    @Inject(
            method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD")
    )
    private void arcaneforge$injectNitwitTrades(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Villager villager = (Villager) (Object) this;
        if (villager.level().isClientSide()) return;
        if (!villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) return;

        boolean isMinero;
        if (villager.getOffers().isEmpty()) {
            isMinero = ThreadLocalRandom.current().nextBoolean();
            villager.setOffers(isMinero ? ArcaneMinerTrades.buildOffers() : ArcaneVillagerTrades.buildInitialOffers());
        } else {
            isMinero = isMineroRole(villager);
            // Cada vez que le hablas, revisa si ya te gano el siguiente tramo/tier.
            if (isMinero) {
                ArcaneMinerTrades.updateOffers(villager.getOffers());
            } else {
                ArcaneVillagerTrades.updateOffers(villager.getOffers());
            }
        }

        // Le ponemos nombre para poder detectarlo en ArcaneMerchantScreenMixin
        // y para que se vea en el titulo del trade, pero SIN nametag flotando
        // sobre su cabeza en el mundo. setCustomNameVisible(false) explicito
        // (no basta con no llamarlo) porque algunos aldeanos ya se habian
        // guardado con CustomNameVisible=true de una version anterior de este
        // mixin, y ese valor persiste en el mundo hasta que se lo corrige.
        Component name = isMinero
                ? Component.literal("Aldeano Minero ").append(ArcaneMinerTrades.trustLabel(villager.getOffers()))
                : Component.literal("Aldeano Arcano ").append(ArcaneVillagerTrades.trustLabel(villager.getOffers()));
        villager.setCustomName(name);
        villager.setCustomNameVisible(false);
    }

    private static boolean isMineroRole(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getCostA().is(Items.COOKED_BEEF)) return true;
        }
        return false;
    }
}
