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
 * Guia de encantamientos arcanos. Clic derecho -> entrega el libro escrito.
 *
 * LEGIBILIDAD:
 *  - Cuerpo (logica y explicacion) en NEGRO puro: sin codigo de color = maximo
 *    contraste sobre el papel claro. Es lo que pidio el usuario.
 *  - Titulos en azul/rojo oscuro y negrita solo para separar secciones.
 *  - Sin tildes: la fuente del libro las renderiza apretadas.
 *  - Lineas cortas (<=16 chars) y espaciadas.
 *  - Cubre los 12 encantamientos del mod, uno por uno.
 */
public class ArcaneGuideBook extends Item {

    public ArcaneGuideBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // Desbloquea las recetas del mod en el libro de recetas.
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.Andres.arcaneforge.event.ArcaneStartupHandler.unlockArcaneRecipes(sp);
            }
            // Entrega un LIBRO ESCRITO leible con la guia, pero solo si el jugador
            // no tiene ya uno (evita que se acumulen al hacer clic varias veces).
            if (!hasGuideAlready(player)) {
                ItemStack book = createGuideBook();
                if (!player.addItem(book)) {
                    player.drop(book, false);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** True si el jugador ya tiene en el inventario el libro escrito de la guia. */
    private static boolean hasGuideAlready(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.WRITTEN_BOOK)
                    && stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        Filterable<String> title = Filterable.passThrough("Guia Arcana");

        List<Filterable<Component>> pages = List.of(
            // 1 — Portada
            page("§1§lARCANE FORGE§r\n\n\nGuia de\nEncantamientos\nArcanos\n\n\nBy Andres"),

            // 2 — Indice (bloques)
            page("§1§lBLOQUES§r\n\np3 Mesa\np4 Pedestal\np5 B. de Poder\np6 Varita\np7 Encantar"),

            // 3 — Mesa Arcana
            page("§1§lMESA ARCANA§r\n\nCrafteo:\n2 Netherite\n4 Obsidiana\n2 Manzana Dor.\n  Encantada\n1 Nether Star\n1 Estanteria"),

            // 4 — Pedestal
            page("§1§lPEDESTAL§r\n\nCrafteo:\n2 Amatista\n1 Ojo Ender\n4 Obsidiana\n\nPon un Bloque\nde Poder al\nlado para\npotenciar."),

            // 5 — Bloque de Poder
            page("§1§lB. DE PODER§r\n\nCrafteo:\n4 End Crystal\n4 Netherite\n1 Nether Star\n\nSube el nivel\nmaximo a 255."),

            // 6 — Varita
            page("§1§lVARITA§r\n\nCrafteo:\n2 Ender Pearl\n1 Amatista\n2 Palos\n\nClic en cofres\npara vincular\nsus materiales\na la Mesa."),

            // 7 — Como encantar
            page("§1§lENCANTAR§r\n\n1 Pon la Mesa\n2 Vincula\n  cofres\n3 Llena los\n  cofres\n4 Abre y elige\n  el encanto"),

            // 8 — Indice encantos
            page("§1§lENCANTOS§r\n\np9  Juicio\np10 Cataclismo\np11 Repulsion\np12 Trueno\np13 Festin\np14 Gancho"),

            // 9 — Juicio Apocaliptico
            page("§4§lJUICIO§r\n§4§lAPOCALIPTICO§r\n\nArco o Ballesta\nNivel max 3\n\nCada flecha\ninvoca rayos\nal impactar.\nEl arco no se\nrompe."),

            // 10 — Cataclismo Arcano
            page("§4§lCATACLISMO§r\n§4§lARCANO§r\n\nMazo\nNivel max 3\n\nGolpe con\nempuje en area\ny explosion.\nDano sube por\nnivel."),

            // 11 — Repulsion Arcana
            page("§4§lREPULSION§r\n§4§lARCANA§r\n\nEscudo\nNivel max 3\n\nAl bloquear:\nrefleja dano\ny empuja a\nlos enemigos."),

            // 12 — Trueno Encadenado
            page("§4§lTRUENO§r\n§4§lENCADENADO§r\n\nTridente\nNivel max 3\n\nAl impactar,\nrayos que\nsaltan entre\nenemigos\ncercanos."),

            // 13 — Festin Eterno
            page("§4§lFESTIN§r\n§4§lETERNO§r\n\nComida\nNivel max 1\n\nHambre siempre\nllena. Cada\n30s da efectos\nal azar, buenos\no malos."),

            // 14 — Gancho Etereo
            page("§4§lGANCHO§r\n§4§lETEREO§r\n\nCana de Pescar\nNivel max 3\n\nPesca botin\nraro: diamante,\nesmeralda,\nperlas y mas."),

            // 15 — Indice 2
            page("§1§lMAS ENCANTOS§r\n\np16 Excavacion\np17 Cosecha\np18 Alas\np19 Caminata\np20 Lanzamiento\np21 Proteccion\np22 Fundicion"),

            // 16 — Soul Delve
            page("§4§lEXCAVACION§r\n§4§lDE ALMAS§r\n\nPico o Pala\nNivel max 3\n\nAl minar da\nXP extra y\npuede duplicar\nlo que sueltan\nlos bloques."),

            // 17 — Soul Harvest
            page("§4§lCOSECHA§r\n§4§lDE ALMAS§r\n\nAzada\nNivel max 3\n\nCosecha en\narea, replanta\ny los cultivos\nvan solos a tu\ninventario."),

            // 18 — Void Wings
            page("§4§lALAS DEL§r\n§4§lVACIO§r\n\nElitro\nNivel max 3\n\nSin dano de\ncaida y mas\nvelocidad al\nvolar."),

            // 19 — Ethereal Walk
            page("§4§lCAMINATA§r\n§4§lETEREA§r\n\nArmadura\nNivel max 1\n\nMovimiento\nmejorado y\nmenos estorbo\nal caminar."),

            // 20 — Ethereal Launch
            page("§4§lLANZAMIENTO§r\n§4§lETEREO§r\n\nTridente\nNivel max 5\n\nImpulso fuerte\nal lanzar o\nal usar el\ntridente."),

            // 21 — Void Protection
            page("§4§lPROTECCION§r\n§4§lDEL VACIO§r\n\nNivel max 3\n\nReduce el dano\nrecibido y\nprotege contra\nefectos del\nvacio."),

            // 22 — Fundicion Arcana (nuevo)
            page("§4§lFUNDICION§r\n§4§lARCANA§r\n\nPico\nNivel max 1\n\nAl picar menas\nsalen ya\nfundidas. Con\nFortuna salen\nmas lingotes."),

            // 23 — Reparacion Arcana (nuevo)
            page("§4§lREPARACION§r\n§4§lARCANA§r\n\nDurabilidad\nNivel max 3\n\nRepara solo el\nobjeto gastando\nTU experiencia.\nMas nivel =\nmas arreglo."),

            // 24 — Consejo: mesa de cosecha
            page("§1§lTIP COSECHA§r\n\nAgachate y haz\nclic con la\nvarita en la\nMesa para que\nla Cosecha\nmande todo a\nsus cofres.")
        );

        WrittenBookContent content = new WrittenBookContent(title, "ArcaneForge", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }
}
