package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.block.ArcaneDiscountBlock;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.network.C2SEnchantPacket;
import com.Andres.arcaneforge.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ArcaneForgeScreen extends AbstractContainerScreen<ArcaneForgeMenu> {

    private static final int VANILLA_W  = 176;
    private static final int VANILLA_H  = 182;
    private static final int GAP        = 4;
    private static final int PANEL_W    = 156;
    private static final int TOTAL_W    = VANILLA_W + GAP + PANEL_W;
    // El panel de la izquierda (inventario vanilla) se queda en VANILLA_H fijo,
    // porque sus slots estan a coordenadas fijas. Pero el panel de la derecha
    // (estadisticas: fuel, EXP, rareza, lista de materiales por cofre) puede
    // necesitar bastante mas alto que eso cuando hay varias categorias de
    // material a la vez, asi que la ventana completa usa esta altura mayor
    // para que nunca se salga del fondo y quede tapado por la hotbar/chat.
    private static final int WINDOW_H   = 300;

    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_H        = 14;
    private static final int LIST_W       = 124;

    // Paleta del panel izquierdo: azul oscuro, ~80% transparente (alpha ~0x33
    // de 0xFF) para que se vea el mundo detras, pero conservando el relieve
    // biselado (bordes un poco mas opacos) para que los slots se sigan
    // distinguiendo con claridad. Los items dibujados encima siempre quedan
    // nitidos porque se pintan en una capa aparte, sobre este fondo.
    private static final int C_PANEL      = 0x33001433;
    private static final int C_LIGHT      = 0x552D5AA8;
    private static final int C_DARK       = 0x66000A1F;
    private static final int C_SLOT       = 0x55152A52;
    private static final int C_SLOT_HOLE  = 0x66000D26;

    /**
     * Lista curada para el Cetro Arcano del Golem (hierro): solo lo que tiene
     * sentido para un brazo cuerpo a cuerpo — encantamientos de combate
     * melee del mod + defensa/utilidad de golem (Vampiro y Coraza Arcana
     * funcionan de verdad sobre cualquier AbstractGolem, ver VampireHandler/
     * ArcaneEnchantsHandler.onCorazaArcana) + los 3 de daño vanilla. Nada de
     * pesca, mineria, agricultura, viaje ni utilidades de jugador: un golem
     * no pesca ni mina, y esos encantamientos no le sirven de nada puesto en
     * su cetro mas alla de sumar numeros sueltos a sus estadisticas sin
     * ningun sentido tematico. Lealtad (vainilla) tambien tiene gancho propio
     * aqui: hace que el golem siga a quien lo vinculo como un perro (ver
     * ArcaneGolemHandler.onGolemFollowOwner) y lo encoge a la mitad para que
     * quepa siguiendolo por huecos de 2 bloques.
     */
    private static final Set<String> GOLEM_MELEE_ENCHANTS = Set.of(
            "arcaneforge:corte_del_vacio", "arcaneforge:filo_insaciable", "arcaneforge:golpe_partidor",
            "arcaneforge:cadena_arcana", "arcaneforge:golpe_dimensional", "arcaneforge:filo_eterno",
            "arcaneforge:marca_del_cazador", "arcaneforge:golpe_sismico", "arcaneforge:sangria_espectral",
            "arcaneforge:ataque_veloz", "arcaneforge:arcane_cataclysm", "arcaneforge:arcane_repulse",
            "arcaneforge:vampiro", "arcaneforge:coraza_arcana",
            "minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods", "minecraft:loyalty"
    );

    /**
     * Lista curada para el Cetro Arcano del Golem de Nieve: lo equivalente
     * pero de distancia/daño — Carga Rapida es el unico que de verdad tiene
     * un gancho propio en el golem de nieve (ver ArcaneSnowGolemHandler),
     * el resto son encantamientos de daño a distancia del mod (rayo/lanza/
     * arco) que aportan a su mismo pozo de nivel total igual que cualquier
     * otro — mas Vampiro/Coraza Arcana, compartidos con el de hierro. Lealtad
     * (vainilla) aqui hace lo mismo que en el de hierro (seguir al dueño,
     * ver ArcaneGolemHandler.onGolemFollowOwner).
     */
    private static final Set<String> GOLEM_RANGED_ENCHANTS = Set.of(
            "minecraft:quick_charge", "arcaneforge:chain_thunder", "arcaneforge:apocalyptic_judgment",
            "arcaneforge:ethereal_launch", "arcaneforge:vampiro", "arcaneforge:coraza_arcana", "minecraft:loyalty"
    );

    /** Todos los encantamientos validos para el item actual, sin filtrar. */
    private final List<EnchantOption> allEnchants = new ArrayList<>();
    /** Subconjunto de allEnchants que coincide con la busqueda (lo que se ve). */
    private final List<EnchantOption> enchants = new ArrayList<>();
    private int selectedIndex  = -1;
    private int selectedLevel  = 1;
    private int scrollOffset   = 0;
    private int subMenuMode    = 0;
    /** true = selectedLevel representa niveles a QUITAR (reembolsa EXP), false = a AÑADIR. */
    private boolean removeMode = false;
    private String searchQuery = "";
    private EditBox searchBox;

    private ItemStack lastItem = ItemStack.EMPTY;
    private boolean lastPedestalCache = false;

    private int displayedChests      = 0;
    private int displayedBookshelves = 0;
    private int displayedMagicFuel   = 0;
    private boolean hasActivePedestal = false;

    private int fuelCommon    = 0;
    private int fuelUncommon  = 0;
    private int fuelRare      = 0;
    private int fuelEpic      = 0;
    private int fuelLegendary = 0;

    private Button btnScrollUp, btnScrollDown, btnEnchant;
    private final Button[] btnRows = new Button[VISIBLE_ROWS];
    private Button btnModePlus, btnModeMinus;
    private Button[] btnLvlUp, btnLvlDown;

    public ArcaneForgeScreen(ArcaneForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public int getImageWidth() {
        return TOTAL_W;
    }

    @Override
    public int getImageHeight() {
        return WINDOW_H;
    }

    @Override
    protected void init() {
        super.init();

        // --- GUI ADAPTABLE A CUALQUIER PANTALLA ---
        // Centra la ventana completa (mesa vanilla + panel del mod) y evita que
        // se salga por los bordes en pantallas estrechas o con GUI Scale alto.
        // leftPos/topPos son los campos que usa AbstractContainerScreen para dibujar.
        int margin = 4;
        this.leftPos = Math.max(margin, (this.width - TOTAL_W) / 2);
        // Si aun asi no cabe a lo ancho, lo pegamos al borde izquierdo con margen.
        if (this.leftPos + TOTAL_W > this.width - margin) {
            this.leftPos = Math.max(margin, this.width - TOTAL_W - margin);
        }
        this.topPos = Math.max(margin, (this.height - WINDOW_H) / 2);

        int px   = getLeftPos() + VANILLA_W + GAP;
        int py   = getTopPos();
        int searchY = py + 38;
        int listY = searchY + 16;
        int listX = px + 6;
        int scrollX = listX + LIST_W + 2;

        searchBox = new EditBox(this.font, listX, searchY, LIST_W, 14, Component.literal("Buscar"));
        searchBox.setMaxLength(32);
        searchBox.setBordered(true);
        searchBox.setHint(Component.literal("§8🔍 Buscar encantamiento..."));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(s -> { searchQuery = s; applyFilter(); });
        addRenderableWidget(searchBox);

        btnScrollUp   = addRenderableWidget(Button.builder(Component.literal("▲"), b -> doScrollUp())
                .bounds(scrollX, listY, 16, ROW_H).build());
        btnScrollDown = addRenderableWidget(Button.builder(Component.literal("▼"), b -> doScrollDown())
                .bounds(scrollX, listY + (VISIBLE_ROWS - 1) * ROW_H, 16, ROW_H).build());

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int row = i;
            btnRows[i] = addRenderableWidget(Button.builder(Component.literal(""), b -> selectRow(row))
                    .bounds(listX, listY + i * ROW_H, LIST_W, ROW_H).build());
        }

        int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;

        btnModePlus  = addRenderableWidget(Button.builder(Component.literal("➕ Nivel"), b -> toggleSub(1))
                .bounds(px + 4, ctrlY, 74, 14).build());
        btnModeMinus = addRenderableWidget(Button.builder(Component.literal("➖ Nivel"), b -> toggleSub(2))
                .bounds(px + 80, ctrlY, 74, 14).build());

        int bw = 36, gap2 = 2;
        int[] upD    = {  1,   5,   10,   9999};
        // downD son magnitudes POSITIVAS (niveles a quitar); 0 = "Todo" (quita el
        // encantamiento por completo). adjustLevelDown() es quien resta.
        int[] downD  = {  1,   5,   10,   0};
        String[] upL   = {"+1", "+5", "+10", "Max"};
        String[] downL = {"-1", "-5", "-10", "Todo"};

        btnLvlUp   = new Button[4];
        btnLvlDown = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int d = upD[i];
            btnLvlUp[i] = addRenderableWidget(Button.builder(Component.literal(upL[i]),
                    b -> adjustLevel(d)).bounds(px + 4 + i * (bw + gap2), ctrlY, bw, 14).build());
            final int dd = downD[i];
            btnLvlDown[i] = addRenderableWidget(Button.builder(Component.literal(downL[i]),
                    b -> adjustLevelDown(dd)).bounds(px + 4 + i * (bw + gap2), ctrlY, bw, 14).build());
        }

        btnEnchant = addRenderableWidget(Button.builder(Component.literal("⚡ ENCHANT"), b -> doEnchant())
                .bounds(px + 8, ctrlY + 18, PANEL_W - 16, 20).build());

        refreshList();
        syncButtons();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // EditBox no consume las teclas de letra normal en su keyPressed (esas
        // llegan aparte via charTyped), asi que sin este parche, escribir "E"
        // en el buscador se interpreta como el atajo de abrir/cerrar
        // inventario y cierra toda la ventana de la Forja a mitad de escribir.
        if (searchBox != null && searchBox.isFocused() && !event.isEscape()) {
            if (searchBox.keyPressed(event)) return true;
            return true; // se traga cualquier otra tecla para que no llegue al atajo de inventario
        }
        return super.keyPressed(event);
    }

    private void doScrollUp()   { if (scrollOffset > 0) { scrollOffset--; syncButtons(); } }
    private void doScrollDown() { if (scrollOffset < enchants.size() - VISIBLE_ROWS) { scrollOffset++; syncButtons(); } }

    private void selectRow(int visRow) {
        int idx = scrollOffset + visRow;
        if (idx >= 0 && idx < enchants.size()) {
            selectedIndex = idx;
            selectedLevel = 1;
            subMenuMode   = 0;
            removeMode    = false;
            syncButtons();
        }
    }

    private void toggleSub(int mode) {
        subMenuMode = (subMenuMode == mode) ? 0 : mode;
        // Al abrir un panel se fija el modo (subir/bajar) y se reinicia
        // selectedLevel, para que no arrastre un valor del otro modo.
        if (subMenuMode == 2 && selectedIndex >= 0 && selectedIndex < enchants.size()) {
            int curLevel = enchants.get(selectedIndex).currentLevel();
            selectedLevel = curLevel > 0 ? 1 : 0;
            removeMode = true;
        } else if (subMenuMode == 1) {
            selectedLevel = Math.max(1, selectedLevel);
            removeMode = false;
        }
        syncButtons();
    }

    private void adjustLevel(int delta) {
        if (selectedIndex < 0) return;
        EnchantOption opt = enchants.get(selectedIndex);
        int currentLevel = opt.currentLevel();

        boolean isCreative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
        int maxLimit = hasActivePedestal ? 255 : 15;

        if (delta == 9999) {
            if (isCreative) {
                selectedLevel = maxLimit - currentLevel;
            } else {
                float enchMult = ArcaneForgeBlockEntity.getEnchantmentMultiplier(opt.id());

                // Mismo descuento del aldeano (rama MATERIAL) que ya se aplica
                // al costo real en el servidor y al texto "Fuel material" de
                // abajo — sin esto, "Max" subestimaba cuantos niveles alcanzan
                // de verdad cerca de un bloque de descuento.
                float materialDiscount = 0f;
                if (Minecraft.getInstance().level != null) {
                    var forgePos = getMenu().getBlockEntity().getBlockPos();
                    materialDiscount = ArcaneDiscountBlock.getBestDiscount(
                            Minecraft.getInstance().level, forgePos, ArcaneDiscountBlock.Branch.MATERIAL);
                }

                int maxPossibleToAdd = 0;
                int fuel = displayedMagicFuel;
                int testLevel = currentLevel;

                while (testLevel < maxLimit) {
                    int baseCostForOne = ArcaneForgeBlockEntity.calculateProgressiveCost(
                            testLevel, 1, displayedBookshelves, hasActivePedestal);
                    int realCostForOne = Math.max(1, Math.round(baseCostForOne * enchMult));
                    if (materialDiscount > 0f) {
                        realCostForOne = Math.max(1, Math.round(realCostForOne * (1.0f - materialDiscount)));
                    }
                    if (fuel >= realCostForOne) {
                        fuel -= realCostForOne;
                        maxPossibleToAdd++;
                        testLevel++;
                    } else {
                        break;
                    }
                }
                selectedLevel = Math.max(1, maxPossibleToAdd);
            }
        } else if (delta == 0) {
            selectedLevel = 1;
        } else {
            selectedLevel = Math.max(1, Math.min(selectedLevel + delta, maxLimit - currentLevel));
        }
        removeMode = false;
        subMenuMode = 0;
        syncButtons();
    }

    /** Version "bajar" de adjustLevel: recorta contra el nivel ACTUAL, no contra el tope. */
    private void adjustLevelDown(int delta) {
        if (selectedIndex < 0) return;
        EnchantOption opt = enchants.get(selectedIndex);
        int currentLevel = opt.currentLevel();

        if (currentLevel <= 0) {
            // Nada que quitar en este encantamiento; no hay panel que abrir de verdad.
            subMenuMode = 0;
            syncButtons();
            return;
        }

        if (delta == 0) {
            selectedLevel = currentLevel; // "Todo": quita el encantamiento por completo
        } else {
            selectedLevel = Math.max(1, Math.min(selectedLevel + delta, currentLevel));
        }
        removeMode = true;
        subMenuMode = 0;
        syncButtons();
    }

    private void doEnchant() {
        if (selectedIndex < 0 || selectedIndex >= enchants.size()) return;
        if (removeMode && selectedLevel <= 0) return;
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        EnchantOption opt = enchants.get(selectedIndex);
        int delta = removeMode ? -selectedLevel : selectedLevel;
        conn.send(new C2SEnchantPacket(
                getMenu().getBlockEntity().getBlockPos(),
                opt.id(),
                delta
        ));
    }

    private void syncButtons() {
        boolean hasSel = selectedIndex >= 0 && selectedIndex < enchants.size();

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx < enchants.size()) {
                EnchantOption opt = enchants.get(idx);
                String prefix = (idx == selectedIndex) ? "► " : "  ";
                String warn   = opt.isCompatible() ? "" : "⚠ ";
                // La estrella dorada ✦ marca SOLO los encantamientos del mod que son
                // naturales para esta herramienta. Los que solo sirven por el pedestal
                // se muestran normales (sin estrella) aunque sean aplicables.
                boolean esDelMod = opt.id().getNamespace().equals(ArcaneForge.MODID);
                String label;
                if (esDelMod && opt.naturalCompat()) {
                    label = prefix + "§6✦ " + warn + opt.displayName() + "§r";
                } else if (esDelMod) {
                    String color = opt.isCompatible() ? "§7" : "§8";  // gris claro / gris
                    label = prefix + color + warn + opt.displayName() + "§r";
                } else {
                    label = prefix + warn + opt.displayName();
                }
                btnRows[i].setMessage(Component.literal(label));
                btnRows[i].visible = true;
                btnRows[i].active  = true;
                // Mini ventana con la explicacion del encantamiento al pasar el mouse.
                // Tenemos texto ".desc" escrito a mano en los lang tanto para los
                // encantamientos del mod como para todos los vanilla (namespace
                // "minecraft"); si algun otro mod externo aportara encantamientos
                // sin traduccion propia, no ponemos tooltip para evitar mostrar la
                // clave cruda sin traducir.
                String ns = opt.id().getNamespace();
                btnRows[i].setTooltip((esDelMod || ns.equals("minecraft"))
                        ? Tooltip.create(Component.translatable("enchantment." + ns + "." + opt.id().getPath() + ".desc"))
                        : null);
            } else {
                btnRows[i].visible = false;
                btnRows[i].setTooltip(null);
            }
        }

        btnScrollUp.active   = scrollOffset > 0;
        btnScrollDown.active = scrollOffset < Math.max(0, enchants.size() - VISIBLE_ROWS);

        int curLevelForSel = hasSel ? enchants.get(selectedIndex).currentLevel() : 0;

        boolean masterVisible = hasSel && subMenuMode == 0;
        btnModePlus.visible  = masterVisible || !hasSel;
        btnModeMinus.visible = masterVisible || !hasSel;
        btnModePlus.active   = hasSel;
        // Sin nivel actual no hay nada que bajar.
        btnModeMinus.active  = hasSel && curLevelForSel > 0;

        for (int i = 0; i < 4; i++) {
            btnLvlUp[i].visible   = hasSel && subMenuMode == 1;
            btnLvlDown[i].visible = hasSel && subMenuMode == 2;
        }

        btnEnchant.active = hasSel && (!removeMode || selectedLevel > 0);
        btnEnchant.setMessage(Component.literal(
                !hasSel ? "⚡ ENCHANT"
                        : removeMode ? "♦ QUITAR -" + fmtNum(selectedLevel) + " (+EXP)"
                                     : "⚡ ENCHANT +" + fmtNum(selectedLevel)));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        try {
            ItemStack cur = getMenu().getSlot(0).getItem();
            boolean pedestalChanged = this.hasActivePedestal != this.lastPedestalCache;

            if (!ItemStack.isSameItemSameComponents(cur, lastItem) || pedestalChanged) {
                lastItem = cur.copy();
                this.lastPedestalCache = this.hasActivePedestal;
                refreshList();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Reconstruye allEnchants desde cero (consulta el registro) y luego
     * reaplica el filtro de busqueda. La seleccion actual y la posicion del
     * scroll se conservan por IDENTIDAD del encantamiento (no por indice):
     * antes, cada vez que esto se llamaba (por ejemplo justo despues de
     * encantar, cuando el item cambia de componentes) el scroll se reseteaba
     * a 0 sin condicion, obligando al jugador a bajar de nuevo para
     * encontrar el mismo encantamiento que acababa de subir de nivel.
     */
    private void refreshList() {
        Identifier prevSelId = (selectedIndex >= 0 && selectedIndex < enchants.size())
                ? enchants.get(selectedIndex).id() : null;

        allEnchants.clear();
        try {
            var item = getMenu().getSlot(0).getItem();
            if (item.isEmpty() || Minecraft.getInstance().level == null) {
                enchants.clear();
                selectedIndex = -1; selectedLevel = 1; scrollOffset = 0; subMenuMode = 0;
                syncButtons();
                return;
            }

            var reg = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemEnchantments currentEnchants = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            boolean isTotem  = item.is(Items.TOTEM_OF_UNDYING);
            boolean isRanged = item.is(Items.BOW) || item.is(Items.CROSSBOW);
            boolean isGolemRod     = item.is(ModItems.GOLEM_BINDING_ROD.get());
            boolean isSnowGolemRod = item.is(ModItems.SNOW_GOLEM_BINDING_ROD.get());
            boolean isGeneratorCore = item.is(ModItems.GENERATOR_CORE.get());

            // Items "especiales" (varas/libros custom: Cetro del Golem, Vara de
            // Vinculacion, Guia Arcana, etc.) no encajan en NINGUNA categoria de
            // encantamiento de vainilla (canEnchant() da false para todos), asi
            // que "lo natural para su herramienta" no significa nada para ellos
            // — filtrarlos igual que una espada dejaba la lista vacia por
            // defecto (el bug: nada pasaba el filtro, ENCHANT quedaba sin nada
            // que aplicar). Si el item no tiene NINGUN encantamiento vanillaCompat,
            // se trata como universal: se ve la lista completa sin buscar.
            boolean anyVanillaCompat = false;
            for (var h0 : reg.listElements().toList()) {
                try {
                    if (h0.isBound() && h0.value().canEnchant(item)) { anyVanillaCompat = true; break; }
                } catch (Exception ignored) {}
            }
            boolean universalItem = !anyVanillaCompat && !isTotem;

            reg.listElements().forEach(h -> {
                try {
                    if (!h.isBound()) return;

                    Identifier id = h.key().identifier();
                    boolean isOurTotemEnchant = id.getNamespace().equals(ArcaneForge.MODID) && id.getPath().equals("void_protection");
                    boolean isApocalyptic     = id.getNamespace().equals(ArcaneForge.MODID) && id.getPath().equals("apocalyptic_judgment");

                    if (isTotem  && !isOurTotemEnchant) return;
                    if (!isTotem && isOurTotemEnchant)  return;
                    // Apocaliptico solo cuenta como "ranged real" con un arco/ballesta
                    // puesto; con el cetro del golem de nieve pasa igual por la lista
                    // curada de abajo, no por este chequeo de item vanilla.
                    if (isApocalyptic && !isRanged && !isSnowGolemRod) return;
                    if ((isGolemRod || isGeneratorCore) && !GOLEM_MELEE_ENCHANTS.contains(id.toString())) return;
                    if (isSnowGolemRod && !GOLEM_RANGED_ENCHANTS.contains(id.toString())) return;

                    boolean vanillaCompat = false;
                    try { vanillaCompat = h.value().canEnchant(item); }
                    catch (Exception ignored) {}

                    boolean finalCompat = vanillaCompat || universalItem || (hasActivePedestal && (isTotem || !vanillaCompat));
                    if (isApocalyptic) finalCompat = hasActivePedestal;

                    int currentLevel = currentEnchants.getLevel(h);
                    // naturalCompat = compatible de forma "natural" (su herramienta real),
                    // sin contar el empujon del pedestal. La estrella dorada solo se pone
                    // en estos. El totem y el apocaliptico-en-arco cuentan como naturales.
                    boolean naturalCompat = universalItem
                            || vanillaCompat
                            || (isTotem && isOurTotemEnchant)
                            || (isApocalyptic && isRanged);
                    allEnchants.add(new EnchantOption(
                            id,
                            Enchantment.getFullname(h, 1).getString(),
                            h.value().getMaxLevel(), h, finalCompat, currentLevel, naturalCompat));
                } catch (Exception e) {}
            });

            allEnchants.sort((a, b) -> {
                boolean aCustom = a.id().getNamespace().equals(ArcaneForge.MODID);
                boolean bCustom = b.id().getNamespace().equals(ArcaneForge.MODID);
                // 1) Los del MOD NATURALES para esta herramienta (estrella dorada)
                //    van primero del todo.
                boolean aTop = aCustom && a.naturalCompat();
                boolean bTop = bCustom && b.naturalCompat();
                if (aTop != bTop) return aTop ? -1 : 1;
                // 2) Luego el resto de los del mod que sirven por el pedestal.
                boolean aMid = aCustom && a.isCompatible();
                boolean bMid = bCustom && b.isCompatible();
                if (aMid != bMid) return aMid ? -1 : 1;
                // 3) Despues, el resto de los del mod (aunque no sean compatibles).
                if (aCustom != bCustom) return aCustom ? -1 : 1;
                // 4) Dentro del mismo grupo, los compatibles antes que los que no.
                if (a.isCompatible() != b.isCompatible()) return a.isCompatible() ? -1 : 1;
                // 5) Y por nombre, para un orden estable.
                return a.displayName().compareToIgnoreCase(b.displayName());
            });
        } catch (Exception e) {
            ArcaneForge.LOGGER.error("Error al refrescar la lista de la forja: ", e);
        }

        applyFilter(prevSelId);
    }

    /** Reconstruye "enchants" (la lista visible) a partir de allEnchants + searchQuery. */
    private void applyFilter() {
        applyFilter((selectedIndex >= 0 && selectedIndex < enchants.size())
                ? enchants.get(selectedIndex).id() : null);
    }

    private void applyFilter(Identifier prevSelId) {
        enchants.clear();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (EnchantOption opt : allEnchants) {
            boolean matchesQuery = q.isEmpty() || opt.displayName().toLowerCase(Locale.ROOT).contains(q);
            if (!matchesQuery) continue;
            // Sin busqueda escrita, solo se ve lo naturalmente compatible con el
            // item puesto (o lo que ya sirve para "cualquier cosa", que siempre
            // cuenta como natural). Escribir en el buscador SI revela el resto
            // (por ejemplo "fortuna" con una espada puesta) — asi el jugador
            // sigue pudiendo llegar a ellos a proposito, solo que ya no le
            // aparecen sin pedirlo.
            if (q.isEmpty() && !opt.naturalCompat()) continue;
            enchants.add(opt);
        }

        selectedIndex = -1;
        if (prevSelId != null) {
            for (int i = 0; i < enchants.size(); i++) {
                if (enchants.get(i).id().equals(prevSelId)) { selectedIndex = i; break; }
            }
        }
        if (selectedIndex < 0) { selectedLevel = 1; subMenuMode = 0; removeMode = false; }

        int maxScroll = Math.max(0, enchants.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        syncButtons();
    }

    // Dibuja un rectangulo con bisel estilo vanilla (claro arriba/izq, oscuro abajo/der).
    // Misma tecnica que DimensionalBagScreen, para que todo el mod comparta un solo estilo.
    private void beveledPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, C_PANEL);
        g.fill(x, y, x + w, y + 1, C_LIGHT);
        g.fill(x, y, x + 1, y + h, C_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, C_DARK);
        g.fill(x + w - 1, y, x + w, y + h, C_DARK);
    }

    private void drawSlotHole(GuiGraphicsExtractor g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, C_SLOT);
        g.fill(sx, sy, sx + 16, sy + 16, C_SLOT_HOLE);
    }

    private void drawSlots(GuiGraphicsExtractor g, int gx, int gy, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawSlotHole(g, gx + col * 18, gy + row * 18);
            }
        }
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = getLeftPos();
        int y = getTopPos();

        // Panel izquierdo estilo vanilla: antes se dibujaba con una textura
        // (arcane_forge.png) que en realidad era el placeholder "textura
        // faltante" de Minecraft (rombo negro/magenta), por eso salia negro
        // en el juego. Se reemplaza por el mismo estilo dibujado a mano que
        // usa el Saco Dimensional, encajando con el slot de encantar (80,35)
        // y el inventario del jugador definidos en ArcaneForgeMenu.
        beveledPanel(graphics, x, y, VANILLA_W, VANILLA_H);
        drawSlotHole(graphics, x + 80, y + 35);   // slot de encantar
        drawSlots(graphics, x + 8, y + 84, 9, 3);  // inventario del jugador
        drawSlots(graphics, x + 8, y + 142, 9, 1); // hotbar

        int px = x + VANILLA_W + GAP;
        // Solo la zona de arriba (titulo, lista, botones) lleva fondo solido.
        // La zona de abajo (fuel/EXP/rareza/materiales) se queda transparente
        // a proposito -- WINDOW_H solo le da espacio de sobra para no
        // encimarse con la hotbar, no dibuja una caja detras.
        graphics.fill(px, y, px + PANEL_W, y + VANILLA_H, 0xDD111122);
        graphics.fill(px, y, px + PANEL_W, y + 2, 0xFFFFAA00);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int px = getLeftPos() + VANILLA_W + GAP;
        int py = getTopPos();

        graphics.text(this.font, "⚡ Arcane Forge ⚡", px + 8, py + 6, 0xFFFFAA00);

        refreshClientData();

        ItemStack cur = getMenu().getSlot(0).getItem();
        if (!ItemStack.isSameItemSameComponents(cur, lastItem) || this.hasActivePedestal != this.lastPedestalCache) {
            lastItem = cur.copy();
            this.lastPedestalCache = this.hasActivePedestal;
            refreshList();
        }

        graphics.text(this.font, "Cofres: " + displayedChests + "/" + Config.MAX_LINKED_CHESTS, px + 8, py + 17, 0xFFAAFFAA);
        graphics.text(this.font, "Librerías: " + displayedBookshelves, px + 8, py + 27, 0xFF8888FF);

        int searchY = py + 38;
        int listY = searchY + 16;
        graphics.fill(px + 4, searchY - 2, px + PANEL_W - 4, listY + VISIBLE_ROWS * ROW_H + 2, 0xBB000022);

        if (selectedIndex >= 0 && selectedIndex < enchants.size()) {
            int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;
            EnchantOption opt = enchants.get(selectedIndex);

            float enchMult = ArcaneForgeBlockEntity.getEnchantmentMultiplier(opt.id());

            // Descuentos activos por los bloques del aldeano cerca de la Forja
            // (se muestran igual en ambos modos, aunque solo afecten al costo
            // de subir; se calculan una sola vez aqui).
            float materialDiscount = 0f, xpDiscount = 0f;
            if (Minecraft.getInstance().level != null) {
                var forgePos = getMenu().getBlockEntity().getBlockPos();
                materialDiscount = ArcaneDiscountBlock.getBestDiscount(Minecraft.getInstance().level, forgePos, ArcaneDiscountBlock.Branch.MATERIAL);
                xpDiscount       = ArcaneDiscountBlock.getBestDiscount(Minecraft.getInstance().level, forgePos, ArcaneDiscountBlock.Branch.XP);
            }
            boolean isCreative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();

            if (removeMode) {
                // Modo "bajar nivel": sin costo de fuel, reembolsa EXP (misma
                // formula que costaria volver a subir esos niveles).
                int xpRefund = Math.max(1, (int) (selectedLevel * 3 * enchMult));
                int nuevoNivel = Math.max(0, opt.currentLevel() - selectedLevel);
                graphics.text(this.font, "♦ Quitar " + fmtNum(selectedLevel) + " nivel(es) → nivel " + nuevoNivel, px + 8, ctrlY + 36, 0xFFFF8888);
                graphics.text(this.font, "Reembolso: §a+" + xpRefund + " EXP§r", px + 8, ctrlY + 46, 0xFF55FF55);
            } else {
                int baseCost  = ArcaneForgeBlockEntity.calculateProgressiveCost(opt.currentLevel(), selectedLevel, displayedBookshelves, hasActivePedestal);
                int totalCost  = Math.max(1, Math.round(baseCost * enchMult));

                if (materialDiscount > 0f) totalCost = Math.max(1, Math.round(totalCost * (1.0f - materialDiscount)));
                if (isCreative) totalCost = 0;

                boolean canAfford = isCreative || (displayedMagicFuel >= totalCost);

                graphics.text(this.font, "Fuel material: " + fmtNum(totalCost), px + 8, ctrlY + 36, canAfford ? 0xFF55FF55 : 0xFFFF5555);

                if (!isCreative && Minecraft.getInstance().player != null) {
                    int xpCost = Math.max(1, (int)(selectedLevel * 3 * enchMult));
                    if (xpDiscount > 0f) xpCost = Math.max(1, Math.round(xpCost * (1.0f - xpDiscount)));
                    int playerXP = Minecraft.getInstance().player.experienceLevel;
                    boolean canAffordXP = playerXP >= xpCost;
                    graphics.text(this.font, "EXP: -" + xpCost + " lvl (tienes " + playerXP + ")", px + 8, ctrlY + 46, canAffordXP ? 0xFFFFFF55 : 0xFFFF5555);
                } else if (isCreative) {
                    graphics.text(this.font, "EXP: Gratis (Creativo)", px + 8, ctrlY + 46, 0xFF55FF55);
                }
            }

            // Umbrales en vez de igualdad exacta: la vieja version solo cubria
            // 1.0/2.5/3.0 y caia a "x5 LEGENDARIO" para cualquier otro valor,
            // asi que night_vision (x4) y miners_sight (x2) — ver
            // ArcaneForgeBlockEntity.getEnchantmentMultiplier — se mostraban
            // mal en la GUI (etiqueta y color no coincidian con el multiplicador
            // real que de hecho se cobra).
            String multStr = enchMult >= 5.0f ? "x5 (LEGENDARIO)"
                    : enchMult >= 4.0f ? "x4 (Especial)"
                    : enchMult >= 3.0f ? "x3 (Mod Arcano)"
                    : enchMult >= 2.5f ? "x2.5 (Raro vanilla)"
                    : enchMult >= 2.0f ? "x2 (Utilidad)"
                    : "x1 (Común)";
            int multColor  = enchMult >= 5.0f ? 0xFFFF00FF : enchMult >= 3.0f ? 0xFF8800FF : enchMult >= 2.5f ? 0xFF00FFFF : 0xFFFFFFFF;
            graphics.text(this.font, "Rareza: " + multStr, px + 8, ctrlY + 56, multColor);

            int statsYOff = ctrlY + 56;
            if (materialDiscount > 0f || xpDiscount > 0f) {
                statsYOff += 10;
                StringBuilder discLine = new StringBuilder("Descuento aldeano: ");
                if (materialDiscount > 0f) discLine.append("-").append(Math.round(materialDiscount * 100)).append("% mat");
                if (materialDiscount > 0f && xpDiscount > 0f) discLine.append("  ");
                if (xpDiscount > 0f) discLine.append("-").append(Math.round(xpDiscount * 100)).append("% exp");
                graphics.text(this.font, discLine.toString(), px + 8, statsYOff, 0xFF55FFAA);
            }

            int yOff = statsYOff + 10;
            graphics.text(this.font, "— Materiales en cofres —", px + 8, yOff, 0xFFCCCCCC);
            yOff += 9;

            if (fuelCommon > 0)    { graphics.text(this.font, "§7Común:      " + fmtNum(fuelCommon),    px + 8, yOff, 0xFFAAAAAA); yOff += 9; }
            if (fuelUncommon > 0)  { graphics.text(this.font, "§aPoco común: " + fmtNum(fuelUncommon), px + 8, yOff, 0xFF55FF55); yOff += 9; }
            if (fuelRare > 0)      { graphics.text(this.font, "§bRaro:        " + fmtNum(fuelRare),     px + 8, yOff, 0xFF55FFFF); yOff += 9; }
            if (fuelEpic > 0)      { graphics.text(this.font, "§dÉpico:       " + fmtNum(fuelEpic),     px + 8, yOff, 0xFFFF55FF); yOff += 9; }
            if (fuelLegendary > 0) { graphics.text(this.font, "§5Legendario:  " + fmtNum(fuelLegendary),px + 8, yOff, 0xFFAA00FF); yOff += 9; }

            graphics.text(this.font, "Total fuel: §a" + (isCreative ? "∞ (Creativo)" : fmtNum(displayedMagicFuel)), px + 8, yOff, 0xFFFFFFFF);
            yOff += 9;

            // ⚙️ SECCIÓN DINÁMICA DE PEDESTAL REPARADA
            if (hasActivePedestal) {
                graphics.text(this.font, "✓ Pedestal Activo (Max 255)", px + 8, yOff, 0xFF55FFFF);
            } else {
                graphics.text(this.font, "✗ Sin Pedestal (Max 15)", px + 8, yOff, 0xFFFF5555);
            }
            yOff += 9;

            if (!enchants.get(selectedIndex).isCompatible()) {
                graphics.text(this.font, "⚠ Incompatible vanilla", px + 8, yOff, 0xFFFF8800);
            }
        } else {
            int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;
            int yOff  = ctrlY + 36;
            graphics.text(this.font, "— Materiales en cofres —", px + 8, yOff, 0xFFCCCCCC);
            yOff += 9;
            if (fuelCommon > 0)    { graphics.text(this.font, "§7Común:      " + fmtNum(fuelCommon),    px + 8, yOff, 0xFFAAAAAA); yOff += 9; }
            if (fuelUncommon > 0)  { graphics.text(this.font, "§aPoco común: " + fmtNum(fuelUncommon), px + 8, yOff, 0xFF55FF55); yOff += 9; }
            if (fuelRare > 0)      { graphics.text(this.font, "§bRaro:        " + fmtNum(fuelRare),     px + 8, yOff, 0xFF55FFFF); yOff += 9; }
            if (fuelEpic > 0)      { graphics.text(this.font, "§dÉpico:       " + fmtNum(fuelEpic),     px + 8, yOff, 0xFFFF55FF); yOff += 9; }
            if (fuelLegendary > 0) { graphics.text(this.font, "§5Legendario:  " + fmtNum(fuelLegendary),px + 8, yOff, 0xFFAA00FF); yOff += 9; }
            graphics.text(this.font, "Total: " + fmtNum(displayedMagicFuel), px + 8, yOff, 0xFFFFFFFF);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, partialTick, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private void refreshClientData() {
        try {
            ArcaneForgeBlockEntity be = getMenu().getBlockEntity();
            if (be != null) {
                displayedChests      = be.getClientLinkedChests();
                displayedBookshelves = be.getClientBookshelves();
                displayedMagicFuel   = be.getClientMagicFuel();
                hasActivePedestal    = be.hasActivePedestalNearby();

                fuelCommon    = be.getClientFuelCommon();
                fuelUncommon  = be.getClientFuelUncommon();
                fuelRare      = be.getClientFuelRare();
                fuelEpic      = be.getClientFuelEpic();
                fuelLegendary = be.getClientFuelLegendary();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (enchants.size() > VISIBLE_ROWS) {
            if (sy > 0) doScrollUp();
            else if (sy < 0) doScrollDown();
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    private static String fmtNum(int n) {
        return n < 1000 ? String.valueOf(n) : String.format("%,d", n);
    }

    private record EnchantOption(Identifier id, String displayName, int vanillaMaxLevel, Holder<Enchantment> holder, boolean isCompatible, int currentLevel, boolean naturalCompat) {}
}