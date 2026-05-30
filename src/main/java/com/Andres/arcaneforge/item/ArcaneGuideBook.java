package com.Andres.arcaneforge.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Guía de encantamientos arcanos — al hacer clic derecho entrega un
 * libro escrito con la guía completa en español/inglés.
 */
public class ArcaneGuideBook extends Item {

    public ArcaneGuideBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack book = createGuideBook();
            if (!player.addItem(book)) {
                player.drop(book, false);
            }
        }
        return InteractionResult.SUCCESS; // Corrected return statement
    }

    public static ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        Filterable<String> title = Filterable.passThrough("Guia Arcana");

        List<Filterable<Component>> pages = List.of(
            // Página 1 — Portada
            page("§6§l✦ ARCANE FORGE ✦§r\n\n§7Guía de Encantamientos\nArcanos\n\nBy Andres\n\n§9Todos los poderes del\nvacío, en tus manos."),
            // Página 2 — Mesa Arcana
            page("§6§lMesa de Enc. Arcana§r\nCrafting:\n2 Netherite + 4 Obsidiana\n+ 2 Manzana Dorada Enc.\n+ Nether Star + Bookshelf\n\n§fSin pedestal: §cMax 15\n§fCon Bloque de Poder:\n§aMax 255"),
            // Página 3 — Bloque de Poder
            page("§6§lBloque de Poder§r\nCrafting:\n4 End Crystal + 4 Netherite\n+ Nether Star (forma X)\n\n§fActiva la Mesa para:\n§a• Más encantamientos\n§a• Nivel hasta 255\n§a• Efectos especiales"),
            // Página 4 — Varita
            page("§6§lVarita de Vinculación§r\nCrafting:\n2 Ender Pearl + 1 Amethyst\n+ 2 Palos (diagonal)\n\nVincula cofres a la Mesa\na distancia para usar\nsus materiales como\ncombustible arcano."),
            // Página 5 — Juicio Apocalíptico
            page("§c§lJuicio Apocalíptico§r\n[Arco / Ballesta]\n\nCada flecha invoca rayos\nen el impacto + explosión.\nEl arco §fnunca se rompe§r\ncon este encantamiento.\n\nRequiere Pedestal."),
            // Página 6 — Anzuelo Etéreo
            page("§3§lAnzuelo Etéreo§r\n[Caña de Pescar]\n\nCada pesca da drops raros:\n§c5% Netherite\n§b15% Diamantes\n§a30% Esmeraldas\n§e50% Ender Pearls\n§665% Blaze Rods\n(acumula por nivel)"),
            // Página 7 — Excavación de Almas
            page("§a§lExcavación de Almas§r\n[Pala / Pico]\n\nAl minar obtienes XP\nextra directamente.\nProbabilidad de duplicar\ndrops basada en nivel."),
            // Página 8 — Cataclismo Arcano
            page("§c§lCataclismo Arcano§r\n[Mazo]\n\nKnockback en área\nal golpear + explosión\narcana pequeña.\nDaño escalable por nivel.\nEnvía enemigos volando."),
            // Página 9 — Alas del Vacío
            page("§b§lAlas del Vacío§r\n[Élitro / Pecho]\n\nSin daño de caída.\nVelocidad al volar.\nNivel 3: élitro nunca\nse rompe mientras vueles."),
            // Página 10 — Trueno en Cadena
            page("§e§lTrueno en Cadena§r\n[Tridente]\n\nAl impactar, rayos en\ncadena entre enemigos\ncercanos (hasta 8).\nMás nivel = más alcance\ny más cadenas."),
            // Página 11 — Repulsión Arcana
            page("§9§lRepulsión Arcana§r\n[Escudo]\n\nAl bloquear:\n• Knockback AOE\n• Refleja daño al atacante\n• Reduce daño recibido\n\nNivel 3: 30% reducción\nmás 30% reflejo."),
            // Página 12 — Festín Eterno
            page("§6§lFestín Eterno§r\n[Comida]\n\nHambre SIEMPRE llena.\nCada 30s: ruleta de\n10 efectos aleatorios!\n\n§aBuenos: Regen, Fuerza,\nVelocidad, Resistencia,\nJackpot total!\n§cMalos: Náusea, Debilidad")
        );

        WrittenBookContent content = new WrittenBookContent(title, "ArcaneForge", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }
}
