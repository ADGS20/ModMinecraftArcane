package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * VISION NOCTURNA (night_vision) — mientras el jugador lleve puesto un casco
 * con este encantamiento, ve en la oscuridad de forma permanente, sin
 * necesitar pociones ni beacons.
 *
 * Se refresca el efecto vanilla NIGHT_VISION una vez por segundo con una
 * duracion corta (bastante mayor que el intervalo de refresco, asi nunca
 * "parpadea" ni se acerca a expirar) mientras el casco siga puesto. En
 * cuanto se lo quita, simplemente dejamos de refrescarlo: el efecto se
 * apaga solo en unos segundos, como cualquier pocion normal. Nunca
 * llamamos a removeEffect(), asi que jamas cancelamos una pocion de vision
 * nocturna real que el jugador se haya tomado por su cuenta.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class NightVisionHandler {

    private static final ResourceKey<Enchantment> NIGHT_VISION_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "night_vision"));

    private static final int CHECK_INTERVAL_TICKS = 20;       // cada 1s
    private static final int EFFECT_DURATION_TICKS = 15 * 20; // 15s, se refresca mucho antes de que expire

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        try {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) return;
            if (getLevel(player, helmet) <= 0) return;

            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    EFFECT_DURATION_TICKS, 0, true, false, true));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("NightVision error: {}", e.getMessage());
        }
    }

    private static int getLevel(Player player, ItemStack helmet) {
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> opt = registry.get(NIGHT_VISION_KEY);
            if (opt.isEmpty()) return 0;
            ItemEnchantments enchants = helmet.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return enchants.getLevel(opt.get());
        } catch (Exception e) {
            return 0;
        }
    }
}
