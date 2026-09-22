package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.registry.ModBlocks;
import com.Andres.arcaneforge.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Trades del Aldeano Minero (el otro rol posible para el Nitwit, ver
 * ArcaneNitwitTradeMixin). Es una escalera de 8 tramos, cada uno
 * desbloqueado solo despues de comprar el anterior (mismo mecanismo de
 * ArcaneVillagerTrades.wasBought/unlockNext, aqui en una sola cadena).
 *
 * NO cobra en carne: un jugador puede automatizar una granja de
 * animales+cocina y volver el precio irrelevante sin ningun esfuerzo real.
 * En su lugar cada tramo pide un material que solo se consigue MINANDO
 * activamente (o luchando algo para conseguirlo), cada vez mas profundo y
 * mas dificil — cobre y piedra roja cerca de la superficie, lapislazuli y
 * cuarzo del Nether, polvo de piedra luminosa, varas de blaze (hay que
 * matar blazes en una fortaleza), y al final escombros ancestrales (lo mas
 * dificil de minar del juego). Nada de esto tiene una granja pasiva
 * infinita en vanilla, asi que el precio siempre cuesta esfuerzo real.
 *
 * El ultimo tramo (8) vende el Nucleo Arcano de Golem: el ingrediente unico
 * que pide la receta del Cetro Arcano del Golem, asi que crear un Golem
 * Arcano (mucho mas fuerte que uno normal) exige haber vaciado toda esta
 * escalera primero.
 */
public final class ArcaneMinerTrades {
    private ArcaneMinerTrades() {}

    public static MerchantOffers buildOffers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(tier1Copper());
        return offers;
    }

    /**
     * Revisa que tramo ya se compro en este aldeano y desbloquea el
     * siguiente si corresponde; tambien "cura" aldeanos que se hayan
     * quedado con tramos de mas (de una version anterior del mod sin este
     * sistema) ocultando de nuevo cualquier tramo cuyo anterior no este
     * comprado.
     */
    public static void updateOffers(MerchantOffers offers) {
        pruneLocked(offers, Items.IRON_INGOT, Items.GOLD_INGOT);
        pruneLocked(offers, Items.GOLD_INGOT, Items.AMETHYST_SHARD);
        pruneLocked(offers, Items.AMETHYST_SHARD, Items.DIAMOND);
        pruneLocked(offers, Items.DIAMOND, Items.EMERALD);
        pruneLocked(offers, Items.EMERALD, ModBlocks.ARCANE_PEDESTAL_ITEM.get());
        pruneLocked(offers, ModBlocks.ARCANE_PEDESTAL_ITEM.get(), ModBlocks.ARCANE_FORGE_ITEM.get());
        pruneLocked(offers, ModBlocks.ARCANE_FORGE_ITEM.get(), ModItems.ARCANE_GOLEM_CORE.get());

        unlockNext(offers, Items.IRON_INGOT, Items.GOLD_INGOT, ArcaneMinerTrades::tier2Redstone);
        unlockNext(offers, Items.GOLD_INGOT, Items.AMETHYST_SHARD, ArcaneMinerTrades::tier3Lapis);
        unlockNext(offers, Items.AMETHYST_SHARD, Items.DIAMOND, ArcaneMinerTrades::tier4Quartz);
        unlockNext(offers, Items.DIAMOND, Items.EMERALD, ArcaneMinerTrades::tier5Glowstone);
        unlockNext(offers, Items.EMERALD, ModBlocks.ARCANE_PEDESTAL_ITEM.get(), ArcaneMinerTrades::tier6Pedestal);
        unlockNext(offers, ModBlocks.ARCANE_PEDESTAL_ITEM.get(), ModBlocks.ARCANE_FORGE_ITEM.get(), ArcaneMinerTrades::tier7Forge);
        unlockNext(offers, ModBlocks.ARCANE_FORGE_ITEM.get(), ModItems.ARCANE_GOLEM_CORE.get(), ArcaneMinerTrades::tier8GolemCore);
    }

    private static boolean wasBought(MerchantOffers offers, Item tierItem) {
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(tierItem) && offer.getUses() > 0) return true;
        }
        return false;
    }

    private static void pruneLocked(MerchantOffers offers, Item prevTierItem, Item nextTierItem) {
        if (!wasBought(offers, prevTierItem)) {
            offers.removeIf(o -> o.getResult().is(nextTierItem));
        }
    }

    private static void unlockNext(MerchantOffers offers, Item prevTierItem, Item nextTierItem, Supplier<MerchantOffer> nextOfferFactory) {
        boolean nextAlreadyOffered = false;
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(nextTierItem)) nextAlreadyOffered = true;
        }
        if (wasBought(offers, prevTierItem) && !nextAlreadyOffered) {
            offers.add(nextOfferFactory.get());
        }
    }

    /** Cuantos de los 8 tramos ya se compraron. */
    public static int countBoughtTiers(MerchantOffers offers) {
        int count = 0;
        if (wasBought(offers, Items.IRON_INGOT)) count++;
        if (wasBought(offers, Items.GOLD_INGOT)) count++;
        if (wasBought(offers, Items.AMETHYST_SHARD)) count++;
        if (wasBought(offers, Items.DIAMOND)) count++;
        if (wasBought(offers, Items.EMERALD)) count++;
        if (wasBought(offers, ModBlocks.ARCANE_PEDESTAL_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_FORGE_ITEM.get())) count++;
        if (wasBought(offers, ModItems.ARCANE_GOLEM_CORE.get())) count++;
        return count;
    }

    /** Nombre de confianza a mostrar sobre el aldeano y en el titulo del trade, segun cuantos tramos ya se compraron. */
    public static Component trustLabel(MerchantOffers offers) {
        int pts = countBoughtTiers(offers);
        if (pts >= 8) return Component.literal("Confianza Máxima").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        if (pts >= 5) return Component.literal("De Confianza").withStyle(ChatFormatting.GREEN);
        if (pts >= 1) return Component.literal("Ganándose la Confianza").withStyle(ChatFormatting.YELLOW);
        return Component.literal("Desconfiado").withStyle(ChatFormatting.GRAY);
    }

    private static MerchantOffer trade(Item costItem, int amount, ItemStack result) {
        return new MerchantOffer(new ItemCost(costItem, amount), result, 999, 0, 0.0f);
    }

    private static MerchantOffer tier1Copper() {
        return trade(Items.RAW_COPPER, 32, new ItemStack(Items.IRON_INGOT, 24));
    }

    private static MerchantOffer tier2Redstone() {
        return trade(Items.REDSTONE, 24, new ItemStack(Items.GOLD_INGOT, 32));
    }

    private static MerchantOffer tier3Lapis() {
        return trade(Items.LAPIS_LAZULI, 16, new ItemStack(Items.AMETHYST_SHARD, 20));
    }

    private static MerchantOffer tier4Quartz() {
        return trade(Items.QUARTZ, 24, new ItemStack(Items.DIAMOND, 40));
    }

    private static MerchantOffer tier5Glowstone() {
        return trade(Items.GLOWSTONE_DUST, 16, new ItemStack(Items.EMERALD, 40));
    }

    private static MerchantOffer tier6Pedestal() {
        return trade(Items.BLAZE_ROD, 4, new ItemStack(ModBlocks.ARCANE_PEDESTAL_ITEM.get()));
    }

    private static MerchantOffer tier7Forge() {
        return trade(Items.ANCIENT_DEBRIS, 3, new ItemStack(ModBlocks.ARCANE_FORGE_ITEM.get()));
    }

    /** El tramo final: el ingrediente que solo el Minero vende, pagado con lo mas duro de minar del juego. */
    private static MerchantOffer tier8GolemCore() {
        return new MerchantOffer(
                new ItemCost(Items.ANCIENT_DEBRIS, 2),
                Optional.of(new ItemCost(Items.BLAZE_ROD, 4)),
                new ItemStack(ModItems.ARCANE_GOLEM_CORE.get()),
                999, 0, 0.0f
        );
    }
}
