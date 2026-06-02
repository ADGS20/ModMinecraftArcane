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
 * Guia de encantamientos arcanos. Clic derecho -> entrega un libro escrito.
 *
 * LEGIBILIDAD (el papel del libro es claro, casi blanco):
 *  - Cuerpo SIN codigo de color = NEGRO puro = maximo contraste y legibilidad.
 *    (El gris §8 se lee peor sobre papel; por eso aqui el cuerpo va en negro.)
 *  - Titulos: §1 (azul oscuro) o §4 (rojo oscuro) en NEGRITA §l. Buen contraste.
 *  - Lineas cortas (<=16 caracteres) para que NUNCA se corten ni se amontonen.
 *  - Lineas en blanco entre bloques para que respire.
 *  - Maximo ~10 lineas por pagina.
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
        return InteractionResult.SUCCESS;
    }

    public static ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        Filterable<String> title = Filterable.passThrough("Guia Arcana");

        List<Filterable<Component>> pages = List.of(
            // 1 — Portada
            page("§1§lARCANE FORGE§r\n\n\nGuia de\nEncantamientos\nArcanos\n\n\nBy Andres"),

            // 2 — Indice
            page("§1§lINDICE§r\n\n3 - Mesa\n4 - Pedestal\n5 - B. de Poder\n6 - Varita\n7 - Encantar\n8 - Encantos"),

            // 3 — Mesa Arcana
            page("§1§lMESA ARCANA§r\n\nCrafteo:\n2 Netherite\n4 Obsidiana\n2 Manzana Dor.\n  Encantada\n1 Nether Star\n1 Estanteria"),

            // 4 — Pedestal
            page("§1§lPEDESTAL§r\n\nCrafteo:\n2 Amatista\n1 Ojo Ender\n4 Obsidiana\n\nPotencia la\nMesa al lado."),

            // 5 — Bloque de Poder
            page("§1§lB. DE PODER§r\n\nCrafteo:\n4 End Crystal\n4 Netherite\n1 Nether Star\n\nSube el nivel\nmaximo a 255."),

            // 6 — Varita
            page("§1§lVARITA§r\n\nCrafteo:\n2 Ender Pearl\n1 Amatista\n2 Palos\n\nClic en cofres\npara vincular."),

            // 7 — Como encantar
            page("§1§lENCANTAR§r\n\n1 Pon la Mesa\n2 Vincula\n  cofres\n3 Llena los\n  cofres\n4 Abre y elige"),

            // 8 — Juicio Apocaliptico
            page("§4§lJUICIO§r\n§4§lAPOCALIPTICO§r\n\nArco / Ballesta\n\nFlechas con\nrayos. El arco\nno se rompe.\n\nNecesita\nPedestal."),

            // 9 — Anzuelo Etereo
            page("§4§lANZUELO§r\n§4§lETEREO§r\n\nCana de Pescar\n\nPesca botin\nraro: diamante,\nesmeralda y\nmas. Sube por\nnivel."),

            // 10 — Excavacion de Almas
            page("§4§lEXCAVACION§r\n§4§lDE ALMAS§r\n\nPala / Pico\n\nMas XP al\nminar. Puede\nduplicar lo\nque sueltan\nlos bloques."),

            // 11 — Cataclismo y Alas
            page("§4§lCATACLISMO§r\nMazo. Empuje\nen area y\nexplosion.\n\n§4§lALAS VACIO§r\nElitro. Sin\ndano de caida\ny mas rapido."),

            // 12 — Trueno, Repulsion, Festin
            page("§4§lMAS ENCANTOS§r\n\nTrueno: rayos\nen cadena.\n(Tridente)\n\nRepulsion:\nempuje al\nbloquear.\n(Escudo)\n\nFestin: comida\ninfinita.")
        );

        WrittenBookContent content = new WrittenBookContent(title, "ArcaneForge", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }
}
