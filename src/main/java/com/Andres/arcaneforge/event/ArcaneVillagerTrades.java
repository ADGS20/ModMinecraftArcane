package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/**
 * Trades del aldeano verde (Nitwit): por defecto, sin necesitar mesa de
 * trabajo cerca, ofrece el Nivel 1 de cada rama (Pedestal = material,
 * Poder = experiencia) a cambio de esmeraldas + item. Cada tier siguiente
 * se desbloquea solo despues de comprarle al menos una vez el tier anterior
 * DE LA MISMA RAMA a este aldeano (se detecta viendo que ese MerchantOffer
 * ya tiene usos > 0).
 *
 * El Nivel 4 ("Confianza Maxima") es especial: solo aparece cuando se han
 * comprado los Niveles 3 de AMBAS ramas, y se paga con un Totem de la
 * Inmortalidad (nada de esmeraldas) + un ingrediente caro tematico de cada
 * rama. La "confianza" que se muestra en el nombre del aldeano es un
 * contador propio del mod (cuantos de los 6 tiers 1-3 ya se compraron),
 * totalmente aparte del nivel de comercio vanilla (Novato/Aprendiz/etc.),
 * que no tiene relacion con este sistema.
 */
public final class ArcaneVillagerTrades {
    private ArcaneVillagerTrades() {}

    public static MerchantOffers buildInitialOffers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(materialTier1());
        offers.add(xpTier1());
        offers.add(powerBlockTrade());
        return offers;
    }

    /**
     * Revisa los tiers ya comprados en este aldeano y desbloquea el siguiente
     * de cada rama si corresponde (incluyendo el Nivel 4 especial cuando
     * ambas ramas llegan a Confianza Maxima). Tambien "cura" aldeanos que se
     * hayan quedado con tiers de mas (por ejemplo, de una version anterior
     * del mod sin este sistema de niveles): si el tier anterior no esta
     * comprado, el siguiente se oculta de nuevo.
     */
    public static void updateOffers(MerchantOffers offers) {
        pruneLocked(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_1_ITEM.get(), ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get());
        pruneLocked(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get(), ModBlocks.ARCANE_PEDESTAL_DISCOUNT_3_ITEM.get());
        pruneLocked(offers, ModBlocks.ARCANE_POWER_DISCOUNT_1_ITEM.get(), ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get());
        pruneLocked(offers, ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get(), ModBlocks.ARCANE_POWER_DISCOUNT_3_ITEM.get());

        unlockNext(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_1_ITEM.get(), ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get(), ArcaneVillagerTrades::materialTier2);
        unlockNext(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get(), ModBlocks.ARCANE_PEDESTAL_DISCOUNT_3_ITEM.get(), ArcaneVillagerTrades::materialTier3);
        unlockNext(offers, ModBlocks.ARCANE_POWER_DISCOUNT_1_ITEM.get(), ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get(), ArcaneVillagerTrades::xpTier2);
        unlockNext(offers, ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get(), ModBlocks.ARCANE_POWER_DISCOUNT_3_ITEM.get(), ArcaneVillagerTrades::xpTier3);

        updateTier4(offers);
    }

    private static void updateTier4(MerchantOffers offers) {
        boolean maxTrust = wasBought(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_3_ITEM.get())
                && wasBought(offers, ModBlocks.ARCANE_POWER_DISCOUNT_3_ITEM.get());

        boolean matTier4Offered = false, xpTier4Offered = false;
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_4_ITEM.get())) matTier4Offered = true;
            if (offer.getResult().is(ModBlocks.ARCANE_POWER_DISCOUNT_4_ITEM.get())) xpTier4Offered = true;
        }

        if (maxTrust) {
            if (!matTier4Offered) offers.add(materialTier4());
            if (!xpTier4Offered) offers.add(xpTier4());
        } else {
            offers.removeIf(o -> o.getResult().is(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_4_ITEM.get())
                    || o.getResult().is(ModBlocks.ARCANE_POWER_DISCOUNT_4_ITEM.get()));
        }
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

    private static void unlockNext(MerchantOffers offers, Item prevTierItem, Item nextTierItem, java.util.function.Supplier<MerchantOffer> nextOfferFactory) {
        boolean nextAlreadyOffered = false;
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(nextTierItem)) nextAlreadyOffered = true;
        }
        if (wasBought(offers, prevTierItem) && !nextAlreadyOffered) {
            offers.add(nextOfferFactory.get());
        }
    }

    /** Cuenta cuantos de los 6 tiers 1-3 (ambas ramas) ya se le compraron a este aldeano. */
    public static int countTrustPoints(MerchantOffers offers) {
        int count = 0;
        if (wasBought(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_1_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_PEDESTAL_DISCOUNT_3_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_POWER_DISCOUNT_1_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get())) count++;
        if (wasBought(offers, ModBlocks.ARCANE_POWER_DISCOUNT_3_ITEM.get())) count++;
        return count;
    }

    /** Nombre de confianza a mostrar sobre el aldeano y en el titulo del trade, segun cuanto se le ha comprado. */
    public static Component trustLabel(MerchantOffers offers) {
        int pts = countTrustPoints(offers);
        if (pts >= 6) return Component.literal("Confianza Máxima").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        if (pts >= 4) return Component.literal("De Confianza").withStyle(ChatFormatting.GREEN);
        if (pts >= 1) return Component.literal("Ganándose la Confianza").withStyle(ChatFormatting.YELLOW);
        return Component.literal("Desconfiado").withStyle(ChatFormatting.GRAY);
    }

    /**
     * Bloque de Poder Arcano barato: craftearlo cuesta 4 cristales de ender +
     * 4 lingotes de netherita + 1 estrella del Nether, pero su unica funcion
     * (los tres bonos de fusion de EnchantmentFusionHandler) no justifica ese
     * precio — el Arcano lo vende directo, sin escalera de confianza, para
     * que valga la pena tenerlo sin tener que craftearlo desde cero.
     */
    private static MerchantOffer powerBlockTrade() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 10), Optional.of(new ItemCost(Items.DIAMOND, 3)),
                new ItemStack(ModBlocks.ARCANE_POWER_BLOCK_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer materialTier1() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.of(new ItemCost(Items.IRON_INGOT, 8)),
                new ItemStack(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_1_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer materialTier2() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 12), Optional.of(new ItemCost(Items.DIAMOND, 4)),
                new ItemStack(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_2_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer materialTier3() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 24), Optional.of(new ItemCost(Items.NETHERITE_INGOT, 1)),
                new ItemStack(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_3_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer materialTier4() {
        return new MerchantOffer(new ItemCost(Items.TOTEM_OF_UNDYING, 1), Optional.of(new ItemCost(Items.DIAMOND_BLOCK, 1)),
                new ItemStack(ModBlocks.ARCANE_PEDESTAL_DISCOUNT_4_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer xpTier1() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.of(new ItemCost(Items.GOLD_INGOT, 8)),
                new ItemStack(ModBlocks.ARCANE_POWER_DISCOUNT_1_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer xpTier2() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 12), Optional.of(new ItemCost(Items.GLOWSTONE, 4)),
                new ItemStack(ModBlocks.ARCANE_POWER_DISCOUNT_2_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer xpTier3() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 24), Optional.of(new ItemCost(Items.NETHER_STAR, 1)),
                new ItemStack(ModBlocks.ARCANE_POWER_DISCOUNT_3_ITEM.get()), 999, 0, 0.0f);
    }

    private static MerchantOffer xpTier4() {
        return new MerchantOffer(new ItemCost(Items.TOTEM_OF_UNDYING, 1), Optional.of(new ItemCost(Items.BEACON, 1)),
                new ItemStack(ModBlocks.ARCANE_POWER_DISCOUNT_4_ITEM.get()), 999, 0, 0.0f);
    }
}
