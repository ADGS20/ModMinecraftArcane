package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Maneja eventos de drops de bloques para aplicar efectos especiales:
 * - Fortuna en hachas para madera (multiplica drops como la fortuna en picos)
 * - Toque de Seda + Fortuna = drops multiplicados del bloque completo
 */
@EventBusSubscriber(modid = ArcaneForge.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AxeFortuneHandler {

    /**
     * Cuando se rompe un bloque de madera con una hacha que tiene Fortuna,
     * multiplica los drops igual que la fortuna en picos para minerales.
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getTool() == null || event.getTool().isEmpty()) return;
        
        ItemStack tool = event.getTool();
        
        // Verificar si es una hacha
        if (!tool.is(Items.WOODEN_AXE) && 
            !tool.is(Items.STONE_AXE) && 
            !tool.is(Items.IRON_AXE) && 
            !tool.is(Items.GOLDEN_AXE) && 
            !tool.is(Items.DIAMOND_AXE) && 
            !tool.is(Items.NETHERITE_AXE)) {
            return;
        }
        
        // Obtener nivel de Fortuna
        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int fortuneLevel = 0;
        
        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> fortuneOpt = 
                registry.getOrThrow(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ENCHANTMENT,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "fortune")
                ));
            
            if (fortuneOpt.isPresent()) {
                fortuneLevel = enchantments.getLevel(fortuneOpt.get());
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.warn("Error obteniendo nivel de fortuna: {}", e.getMessage());
            return;
        }
        
        if (fortuneLevel <= 0) return;
        
        // Verificar si también tiene Toque de Seda
        boolean hasSilkTouch = false;
        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> silkTouchOpt = 
                registry.getOrThrow(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ENCHANTMENT,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "silk_touch")
                ));
            
            if (silkTouchOpt.isPresent() && enchantments.getLevel(silkTouchOpt.get()) > 0) {
                hasSilkTouch = true;
            }
        } catch (Exception e) {
            ArcaneForge.LOGGER.warn("Error verificando toque de seda: {}", e.getMessage());
        }
        
        // Procesar drops
        List<net.neoforged.neoforge.event.level.BlockDropsEvent.Drop> newDrops = new ArrayList<>();
        
        for (net.neoforged.neoforge.event.level.BlockDropsEvent.Drop drop : event.getDrops()) {
            ItemStack dropStack = drop.itemStack();
            
            // Si tiene Toque de Seda, el drop es el bloque completo
            if (hasSilkTouch) {
                // Aplicar fortuna al bloque completo
                int multiplier = getFortuneMultiplier(fortuneLevel);
                for (int i = 0; i < multiplier; i++) {
                    newDrops.add(new net.neoforged.neoforge.event.level.BlockDropsEvent.Drop(dropStack.copy(), drop.pos()));
                }
            } else {
                // Sin Toque de Seda, aplica fortuna normal a los drops
                int multiplier = getFortuneMultiplier(fortuneLevel);
                for (int i = 0; i < multiplier; i++) {
                    newDrops.add(new net.neoforged.neoforge.event.level.BlockDropsEvent.Drop(dropStack.copy(), drop.pos()));
                }
            }
        }
        
        // Reemplazar drops originales con los nuevos
        event.getDrops().clear();
        event.getDrops().addAll(newDrops);
        
        if (fortuneLevel > 0) {
            ArcaneForge.LOGGER.debug("Hacha con Fortuna {} aplicada, drops multiplicados", fortuneLevel);
        }
    }
    
    /**
     * Calcula el multiplicador de fortuna basado en el nivel.
     * Fórmula vanilla: 1 + random.nextInt(nivel + 1) + random.nextInt(nivel)
     * Simplificado: nivel + 1 como promedio
     */
    private static int getFortuneMultiplier(int fortuneLevel) {
        // Usar fórmula similar a vanilla para consistencia
        int base = 1;
        int random1 = (int)(Math.random() * (fortuneLevel + 1));
        int random2 = fortuneLevel > 0 ? (int)(Math.random() * fortuneLevel) : 0;
        return base + random1 + random2;
    }
}
