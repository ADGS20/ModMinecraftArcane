package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * ATAQUE VELOZ — mientras el arma en la mano principal tenga este
 * encantamiento, sube Attributes.ATTACK_SPEED con un modificador temporal.
 * Esto acorta getCurrentItemAttackStrengthDelay() (= 20 / attack_speed),
 * que es el mismo numero que controla la barra de carga del golpe (el
 * "cooldown" de ataque que Mojang metio en 1.9): con suficiente attack
 * speed extra, esa barra se llena casi al instante y cada click vuelve a
 * pegar a daño completo, como en las versiones viejas de Minecraft antes
 * de que existiera esa espera.
 *
 * Se aplica como modificador TRANSITORIO (addOrUpdateTransientModifier),
 * no permanente: se refresca cada tick mientras el arma siga en mano, y se
 * quita solo en el momento en que sueltas el arma o pierde el
 * encantamiento — no hace falta guardar estado propio en ningun lado.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class FastAttackHandler {

    private static final ResourceKey<Enchantment> ATAQUE_VELOZ_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "ataque_veloz"));

    private static final Identifier FAST_ATTACK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "ataque_veloz_bonus");

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
            if (player.level().isClientSide()) return;

            AttributeInstance atkSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
            if (atkSpeedAttr == null) return;

            ItemStack weapon = player.getMainHandItem();
            int level = weapon.isEmpty() ? 0 : lvl(player.level(), weapon, ATAQUE_VELOZ_KEY);

            if (level <= 0) {
                atkSpeedAttr.removeModifier(FAST_ATTACK_MODIFIER_ID);
                return;
            }

            // +5 de velocidad de ataque por nivel: a nivel 3 (5*3=15) sumado
            // a la velocidad base del arma (~1.6 en una espada), el retraso
            // de carga (20 / attack_speed) ya baja a ~1 tick — practicamente
            // instantaneo. Techo re-escalado a 30 (antes 15) para que seguir
            // invirtiendo en la Forja no sea inutil, aunque el efecto util
            // real se sienta identico mucho antes de llegar ahi.
            double bonus = Math.min(5.0 * level, 30.0);
            atkSpeedAttr.addOrUpdateTransientModifier(
                    new AttributeModifier(FAST_ATTACK_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("AtaqueVeloz error: {}", e.getMessage());
        }
    }
}
