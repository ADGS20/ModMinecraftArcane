package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ArcaneForge.MODID, value = Dist.CLIENT)
public class ArcaneVisualHandler {

    // 1. EFECTOS VISUALES: Partículas de colores según el nivel máximo
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();

        CustomData customData = mainHand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.contains("ArcaneAwakened")) {
            if (player.level().isClientSide() && player.level().getGameTime() % 5 == 0) {

                int maxLevel = getMaxEnchantmentLevel(mainHand);
                int color = getArcaneColor(maxLevel);

                // Spawneamos polvo mágico del color exacto
                player.level().addParticle(new DustParticleOptions(color, 1.2f),
                        player.getX() + (player.getRandom().nextDouble() - 0.5),
                        player.getY() + 1.2,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5),
                        0, 0, 0);
            }
        }
    }

    // 2. INTERFAZ: Cambiar el color del nombre del ítem y mostrar cargas del Tótem
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        // A) Cambiar estilo si está Despierto en la Forja
        if (customData.contains("ArcaneAwakened")) {
            int maxLevel = getMaxEnchantmentLevel(stack);
            ChatFormatting textColor = getTooltipColor(maxLevel);

            // Añadimos una estrella y teñimos el nombre original del ítem
            Component originalName = event.getToolTip().get(0);
            event.getToolTip().set(0, Component.literal("✦ ").withStyle(textColor).append(originalName.copy().withStyle(textColor)));
        }

        // B) Mostrar cargas místicas si es nuestro Tótem Encantado
        if (stack.is(Items.TOTEM_OF_UNDYING) && customData.contains("TotemCharges")) {
            CompoundTag tag = customData.copyTag();

            // CORRECCIÓN: Desempaquetamos el Optional usando .orElse(0) igual que en ArcaneTotemKey
            int charges = tag.getInt("TotemCharges").orElse(0);

            // Añadimos líneas decorativas al Tooltip del inventario
            event.getToolTip().add(Component.literal("")); // Línea divisoria en blanco
            event.getToolTip().add(Component.literal("§5✦ Objeto Despierto por la Forja Arcana ✦"));

            // Color dinámico según los usos que le queden
            String colorCharge = charges == 1 ? "§c" : (charges == 2 ? "§e" : "§a");
            event.getToolTip().add(Component.literal("§7⚡ Cargas de vacío: " + colorCharge + charges + "§7 / §b3"));
        }
    }

    // --- MÉTODOS DE UTILIDAD PARA CALCULAR LOS NIVELES ---

    private static int getMaxEnchantmentLevel(ItemStack stack) {
        int max = 0;
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var enchantmentHolder : enchants.keySet()) {
            int lvl = enchants.getLevel(enchantmentHolder);
            if (lvl > max) {
                max = lvl;
            }
        }
        return max;
    }

    private static int getArcaneColor(int level) {
        if (level >= 255) return 0xFF00FF; // Nivel Divino: Magenta
        if (level >= 100) return 0xFF0000; // Nivel Épico: Rojo
        if (level >= 50)  return 0xFFAA00; // Nivel Raro: Naranja
        if (level >= 30)  return 0xFFFF00; // Nivel Alto: Amarillo
        if (level >= 10)  return 0x00FF00; // Nivel Medio: Verde
        return 0x00FFFF;                   // Nivel Bajo: Cyan
    }

    private static ChatFormatting getTooltipColor(int level) {
        if (level >= 255) return ChatFormatting.LIGHT_PURPLE;
        if (level >= 100) return ChatFormatting.RED;
        if (level >= 50)  return ChatFormatting.GOLD;
        if (level >= 30)  return ChatFormatting.YELLOW;
        if (level >= 10)  return ChatFormatting.GREEN;
        return ChatFormatting.AQUA;
    }
}