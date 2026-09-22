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
 *  - Cubre los 40 encantamientos del mod, uno por uno, agrupados por
 *    categoria (espada/lanza, armas especiales, herramientas, armadura,
 *    equipo general, consumibles/contenedores). Mismo texto que usan los
 *    tooltips de la pantalla de encantar (ver lang "*.desc"), condensado a
 *    lineas cortas.
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

        List<Filterable<Component>> pages = new java.util.ArrayList<>(List.of(
            // 1 — Portada
            page("§1§lARCANE FORGE§0\n\n\nGuia de\nEncantamientos\nArcanos\n\n(40 encantos)\n\nBy Andres"),

            // 2 — Indice (bloques)
            page("§1§lBLOQUES§0\n\np3 Mesa\np4 Pedestal\np5 B. de Poder\np6 Varita\np7 Encantar"),

            // 3 — Mesa Arcana
            page("§1§lMESA ARCANA§0\n\nCrafteo:\n2 Netherite\n4 Obsidiana\n2 Manzana Dor.\n  Encantada\n1 Nether Star\n1 Estanteria"),

            // 4 — Pedestal
            page("§1§lPEDESTAL§0\n\nCrafteo:\n2 Amatista\n1 Ojo Ender\n4 Obsidiana\n\nPon un Bloque\nde Poder al\nlado para\npotenciar."),

            // 5 — Bloque de Poder
            page("§1§lB. DE PODER§0\n\nCrafteo:\n4 End Crystal\n4 Netherite\n1 Nether Star\n\nSube el nivel\nmaximo a 255."),

            // 6 — Varita
            page("§1§lVARITA§0\n\nCrafteo:\n2 Ender Pearl\n1 Amatista\n2 Palos\n\nClic en cofres\npara vincular\nsus materiales\na la Mesa."),

            // 7 — Como encantar
            page("§1§lENCANTAR§0\n\n1 Pon la Mesa\n2 Vincula\n  cofres\n3 Llena los\n  cofres\n4 Abre y elige\n  el encanto"),

            // 8 — Indice de categorias (40 encantos en total)
            page("§1§lENCANTOS§0\n(40 en total)\n\np9  Cpo. a cuerpo\np20 Armas espec.\np26 Herramientas\np31 Armadura\np40 Equipo gral\np44 Consumibles\np49 Luna Oscura\np53 Vara Golem")
        ));

        // ═══ CATEGORIA A: CUERPO A CUERPO — espada/lanza/hacha (p9-p19, 11 encantos) ═══
        pages.add(page("§4§lATAQUE§0\n§4§lVELOZ§0\n\nEspada o Lanza\nNivel max 60\n\nGolpeas mucho\nmas rapido sin\nperder dano\npor golpe."));
        pages.add(page("§4§lCADENA§0\n§4§lARCANA§0\n\nEspada\nNivel max 60\n\nParte del dano\nsalta tambien\na enemigos\ncercanos."));
        pages.add(page("§4§lCORTE DEL§0\n§4§lVACIO§0\n\nEspada\nNivel max 250\n\nProbabilidad\nde ignorar\nparte de la\narmadura\nenemiga."));
        pages.add(page("§4§lGOLPE§0\n§4§lDIMENSIONAL§0\n\nEspada\nNivel max 60\n\nProbabilidad\nde teleport.\njusto detras\ndel enemigo."));
        pages.add(page("§4§lFILO§0\n§4§lETERNO§0\n\nEspada\nNivel max 60\n\nGolpes segui-\ndos al mismo\nenemigo hacen\ncada vez mas\ndano."));
        pages.add(page("§4§lMARCA DEL§0\n§4§lCAZADOR§0\n\nEspada\nNivel max 60\n\nProbabilidad\nde marcar: el\nenemigo recibe\nmas dano de\ncualquiera."));
        pages.add(page("§4§lGOLPE§0\n§4§lSISMICO§0\n\nEspada\nNivel max 60\n\nProbabilidad\nde onda que\nempuja y\nralentiza\nalrededor."));
        pages.add(page("§4§lSANGRIA§0\n§4§lESPECTRAL§0\n\nEspada\nNivel max 60\n\nProbabilidad\nde sangrado\n(Marchitez) que\nempeora con\ngolpes segui-\ndos."));
        pages.add(page("§4§lFILO§0\n§4§lINSACIABLE§0\n\nEspada\nNivel max 250\n\nCuanta menos\nvida le quede\nal enemigo,\nmas dano extra\nle haces."));
        pages.add(page("§4§lVAMPIRO§0\n\nEspada\nNivel max 60\n\nRobas parte\ndel dano como\nvida. A vida\nllena, da\ncorazones\nextra."));
        pages.add(page("§4§lGOLPE§0\n§4§lPARTIDOR§0\n\nHacha\nNivel max 250\n\nProbabilidad\nde romper el\nescudo del\nenemigo y\naturdirlo.\nMas nivel:\nmas prob. y\nmas duracion."));

        // ═══ CATEGORIA B: ARMAS ESPECIALES (p20-p25, 6 encantos) ═══
        pages.add(page("§4§lJUICIO§0\n§4§lAPOCALIPTICO§0\n\nArco o Ballesta\nNivel max 250\n\nCada flecha\ninvoca rayos\nal impactar.\nEl arco no se\nrompe."));
        pages.add(page("§4§lCATACLISMO§0\n§4§lARCANO§0\n\nMazo\nNivel max 250\n\nGolpe con\nempuje en area\ny explosion.\nDano sube por\nnivel."));
        pages.add(page("§4§lREPULSION§0\n§4§lARCANA§0\n\nEscudo\nNivel max 250\n\nAl bloquear:\nrefleja dano\ny empuja a\nlos enemigos."));
        pages.add(page("§4§lTRUENO§0\n§4§lENCADENADO§0\n\nTridente\nNivel max 250\n\nAl impactar,\nrayos que\nsaltan entre\nenemigos\ncercanos."));
        pages.add(page("§4§lLANZAMIENTO§0\n§4§lETEREO§0\n\nTridente\nNivel max 10\n\nEl tridente\nvuela mucho\nmas rapido y\nlejos al\nlanzarlo."));
        pages.add(page("§4§lCARGA§0\n§4§lINSTANTANEA§0\n\nBallesta\nNivel max 250\n\nSe carga\nmuchisimo\nmas rapido.\nA nivel alto,\ncasi al\ninstante."));

        // ═══ CATEGORIA C: HERRAMIENTAS (p26-p30, 5 encantos) ═══
        pages.add(page("§4§lEXCAVACION§0\n§4§lDE ALMAS§0\n\nPico o Pala\nNivel max 100\n\nAl minar da\nXP extra y\npuede duplicar\nlo que sueltan\nlos bloques."));
        pages.add(page("§4§lCOSECHA§0\n§4§lDE ALMAS§0\n\nAzada\nNivel max 150\n\nCosecha en\narea, replanta\ny al golpear\nmobs roba\nexperiencia."));
        pages.add(page("§4§lFLORACION§0\n§4§lFERTIL§0\n\nAzada\nNivel max 250\n\nProbabilidad\nde que el\ncultivo cose-\nchado vuelva a\ncrecer al\ninstante."));
        pages.add(page("§4§lFUNDICION§0\n§4§lARCANA§0\n\nPico\nNivel max 175\n\nAl picar menas\nsalen ya\nfundidas. Sin\nnecesitar\nhorno."));
        pages.add(page("§4§lGANCHO§0\n§4§lETEREO§0\n\nCana de Pescar\nNivel max 30\n\nPesca botin\nraro: diamante,\nesmeralda,\nperlas y mas."));

        // ═══ CATEGORIA D: ARMADURA (p31-p39, 9 encantos) ═══
        pages.add(page("§4§lVISION§0\n§4§lMINERA§0\n\nCasco\nNivel max 15\n\nCon la habili-\ndad activada,\nresalta menas\na traves de\nparedes.\nGasta XP."));
        pages.add(page("§4§lVISION§0\n§4§lNOCTURNA§0\n\nCasco\nNivel max 1\n\nVes en la\noscuridad de\nforma\npermanente."));
        pages.add(page("§4§lCAMINATA§0\n§4§lETEREA§0\n\nPeto + Botas\nNivel max 1\n\nAl correr y\nsaltar, creas\nplataformas\ninvisibles\npara cruzar\nel vacio."));
        pages.add(page("§4§lPASO§0\n§4§lNEVADO§0\n\nBotas\nNivel max 1\n\nCaminas sobre\nnieve en polvo\nsin hundirte."));
        pages.add(page("§4§lPASO§0\n§4§lACUATICO§0\n\nBotas\nNivel max 1\n\nCaminas sobre\nel agua sin\nhundirte."));
        pages.add(page("§4§lPASO§0\n§4§lINFERNAL§0\n\nBotas\nNivel max 60\n\nCaminas sobre\nlava sin hun-\ndirte. Inmune\nal fuego y\nla lava."));
        pages.add(page("§4§lALAS DEL§0\n§4§lVACIO§0\n\nElitro\nNivel max 4\n\nSin dano de\ncaida y mas\nvelocidad al\nvolar."));
        pages.add(page("§4§lCORAZA§0\n§4§lARCANA§0\n\nPeto\nNivel max 250\n\nReduce todo el\ndano recibido,\nincluido el\nque ignora\narmadura."));
        pages.add(page("§4§lZANCADA§0\n§4§lFANTASMA§0\n\nPantalones\nNivel max 1\n\nNo activas\nplacas ni\ntrampas, pasos\nsin sonido ni\nvibracion, y\ntu nombre\nqueda oculto."));

        // ═══ CATEGORIA E: EQUIPO GENERAL (p40-p43, 4 encantos) ═══
        pages.add(page("§4§lREPARACION§0\n§4§lARCANA§0\n\nDurabilidad\nNivel max 200\n\nRepara solo el\nobjeto gastando\nTU experiencia.\nMas nivel =\nmas arreglo."));
        pages.add(page("§4§lLEALTAD§0\n§4§lARCANA§0\n\nDurabilidad\nNivel max 1\n\nEl objeto no\nse cae al\nmorir. Lo\nrecuperas al\nreaparecer."));
        pages.add(page("§4§lIMAN§0\n§4§lARCANO§0\n\nSaco o cualquier\nobjeto\nNivel max 60\n\nN1: drops de\nbloques van\na la bolsa.\nN2: drops de\nmobs tambien.\nN3+: recoge del\nsuelo, y el\nradio sigue\ncreciendo con\ncada nivel."));
        pages.add(page("§4§lALMA§0\n§4§lPERDURABLE§0\n\nDurabilidad\nNivel max 1\n\nAl morir no\npierdes tu\nexperiencia.\nReapareces con\nel mismo\nnivel."));

        // ═══ CATEGORIA F: CONSUMIBLES Y CONTENEDORES (p44-p48, 5 encantos) ═══
        pages.add(page("§4§lFESTIN§0\n§4§lETERNO§0\n\nComida\nNivel max 255\n\nHambre siempre\nllena. Cada\ncierto tiempo\nda efectos\nal azar."));
        pages.add(page("§4§lCUBETA§0\n§4§lINFINITA§0\n\nCubetas\nNivel max 1\n\nEl cubo nunca\nse vacia al\nusarlo, ni al\nquemarse como\ncombustible."));
        pages.add(page("§4§lVINCULO§0\n§4§lDIMENSIONAL§0\n\nSaco (Bundle)\nNivel max 1\n\nEspacio casi\ninfinito. Cada\nslot acumula\nhasta\n1 000 000 000\nunidades."));
        pages.add(page("§4§lALMACEN§0\n§4§lINFINITO§0\n\nCofre\nNivel max 1\n\nEl cofre guarda\ncantidades\nenormes de un\nmismo objeto\npor casilla."));
        pages.add(page("§4§lPROTECCION§0\n§4§lDEL VACIO§0\n\nTotem\nNivel max 175\n\nEl totem gana\nmas usos: te\nsalva, te cura\ny aparta a los\nenemigos\ncercanos."));

        // ═══ SISTEMA: LUNA OSCURA/ROJA (evento mundial escalable) ═══
        pages.add(page("§1§lLUNA OSCURA§0\n\nCada 10 dias\nde juego el\nmundo sube de\nronda. Desde\nesa noche los\nmobs hostiles\nempiezan a\nsalir con\narmas y arma-\ndura encanta-\nda, cada ronda\nmas fuerte.\nTecho real:\nnivel 255."));
        pages.add(page("§1§lHERALDOS§0\n\nLa noche que\nsube de ronda\nes mas peli-\ngrosa: pueden\nsalir mobs con\nnombre propio\ny brillo, mas\nfuertes y\nresistentes.\nDerrotarlos da\nbuen botin.\n\nFragmento\nLunar (item):\nadelanta la\nproxima Luna."));

        // ═══ SISTEMA: GENERADOR ARCANO DE GOLEMS ═══
        pages.add(page("§1§lGENERADOR§0\n§1§lDE GOLEMS§0\n\n1 Encanta un\n  Nucleo en la\n  Forja\n2 Clic derecho\n  con el en el\n  Generador\n3 Vincula cofres\n  con la Vara\n  (hierro +\n  calabaza)"));
        pages.add(page("§1§lGOLEMS§0\n§1§lAUTOMATICOS§0\n\nDoble receta\nnormal por golem:\n8 Hierro + 2\nCalabaza Tallada\n(de hierro), o\n4 Nieve + 2\nCalabaza Tallada\n(de nieve).\n\nSalen con los\nmismos encantos\ndel Nucleo, sin\ngastar TU\nexperiencia."));

        // ═══ SISTEMA: VARA DE VINCULO DE GOLEM (manual, sin Generador) ═══
        pages.add(page("§1§lVARA GOLEM§0\n\nCrafteo:\n1 Nucleo\n2 Amatista\n2 Palos\n\nEncantala en\nla Forja, clic\nderecho sobre\nun Golem de\nHierro: le da\ntodos esos\nencantos y se\nrompe."));
        pages.add(page("§1§lVARA GOLEM§0\n§1§lDE NIEVE§0\n\nCrafteo:\n1 Nucleo\n2 Hielo Azul\n2 Palos\n\nMismo uso,\npero sobre un\nGolem de\nNieve. El dano\nde sus bolas\ntambien sube\ncon el nivel."));

        // ═══ TIPS FINALES ═══
        pages.add(page("§1§lTIP COSECHA§0\n\nAgachate y haz\nclic con la\nvarita en la\nMesa para que\nla Cosecha\nmande todo a\nsus cofres."));
        pages.add(page("§1§lUSAR IMAN§0\n\n1 Encanta una\n  bolsa con\n  Vinculo Dim.\n2 Encanta la\n  bolsa con\n  Iman Arcano\n3 Llevala en\n  el inventario\n4 Todo lo que\n  minas o matas\n  va directo\n  a la bolsa."));

        WrittenBookContent content = new WrittenBookContent(title, "ArcaneForge", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }
}
