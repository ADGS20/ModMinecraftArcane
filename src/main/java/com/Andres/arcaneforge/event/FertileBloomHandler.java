package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;
import java.util.Random;

/**
 * FLORACION FERTIL — azada. Al cosechar un cultivo maduro, probabilidad de
 * que vuelva a crecer instantaneamente en el mismo sitio (salta las etapas
 * de crecimiento). Independiente de Cosecha de Almas: si ambos encantos
 * estan en la misma azada, este simplemente puede "mejorar" el replante de
 * Cosecha de Almas (que replanta en edad 0) a edad maxima si la tirada sale
 * bien; si no hay Cosecha de Almas, este es el unico que replanta.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class FertileBloomHandler {

    private static final Random RNG = new Random();

    private static final ResourceKey<Enchantment> FLORACION_FERTIL =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "floracion_fertil"));

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getLevel().isClientSide() || !(event.getBreaker() instanceof ServerPlayer player)) return;

        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof HoeItem)) return;

        BlockState originState = event.getState();
        if (!(originState.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(originState)) return;

        try {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<Holder.Reference<Enchantment>> opt = registry.get(FLORACION_FERTIL);
            if (opt.isEmpty()) return;

            ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            int level = enchants.getLevel(opt.get());
            if (level <= 0) return;

            // "De bandera": ~13% a nivel 1, tope real 70% cerca de nivel 220.
            float chance = Math.min(0.10f + 0.0027f * level, 0.70f);
            if (RNG.nextFloat() >= chance) return;

            Level world = event.getLevel();
            BlockPos pos = event.getPos();
            world.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 3);
        } catch (Exception e) {
            ArcaneForge.LOGGER.debug("FloracionFertil error: {}", e.getMessage());
        }
    }
}
