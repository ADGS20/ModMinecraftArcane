package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import com.Andres.arcaneforge.network.C2SEnchantPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

import java.util.ArrayList;
import java.util.List;

public class ArcaneForgeScreen extends AbstractContainerScreen<ArcaneForgeMenu> {

    // ── Textura ──────────────────────────────────────────────────────────────────
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            ArcaneForge.MOD_ID, "textures/gui/container/arcane_forge.png");

    // Tamaño de la textura PNG (AJUSTA si tu PNG no es 256×256)
    private static final float TEX_W = 256f;
    private static final float TEX_H = 256f;

    // ── Dimensiones del layout ────────────────────────────────────────────────────
    private static final int VANILLA_W  = 176;   // ancho del contenedor vanilla
    private static final int VANILLA_H  = 166;   // alto del contenedor vanilla
    private static final int GAP        = 4;      // espacio entre GUI vanilla y panel
    private static final int PANEL_W    = 156;    // ancho del panel de encantamientos
    private static final int TOTAL_W    = VANILLA_W + GAP + PANEL_W; // 336

    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_H        = 14;
    private static final int LIST_W       = 124;

    // ── Estado ────────────────────────────────────────────────────────────────────
    private final List<EnchantOption> enchants = new ArrayList<>();
    private int selectedIndex  = -1;
    private int selectedLevel  = 1;
    private int scrollOffset   = 0;
    private int subMenuMode    = 0;
    private ItemStack lastItem = ItemStack.EMPTY;

    private int displayedChests      = 0;
    private int displayedBookshelves = 0;
    private int displayedMagicFuel   = 0;
    private boolean hasActivePedestal = false;

    // ── Botones ───────────────────────────────────────────────────────────────────
    private Button btnScrollUp, btnScrollDown, btnEnchant;
    private final Button[] btnRows = new Button[VISIBLE_ROWS];
    private Button btnModePlus, btnModeMinus;
    private Button[] btnLvlUp, btnLvlDown;

    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    public ArcaneForgeScreen(ArcaneForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // CORRECCIÓN: imageWidth es final en 26.1.2 — NO se puede asignar directamente.
        // La solución correcta es sobreescribir getImageWidth() y getImageHeight().
        // NeoForge 26.1 añadió estos getters públicos precisamente para este caso.
        // JEI llama getImageWidth() para saber dónde colocar su panel de recetas.
    }

    // CORRECCIÓN JEI: getImageWidth() devuelve el ancho TOTAL (vanilla + gap + panel).
    // JEI usa este valor para posicionar su panel a la derecha de la GUI.
    // Si devolvemos solo 176, JEI se superpone con el panel de encantamientos.
    @Override
    public int getImageWidth() {
        return TOTAL_W;  // 336 px — JEI colocará sus recetas a partir de aquí
    }

    @Override
    public int getImageHeight() {
        return VANILLA_H;  // 166 px — altura estándar
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // init — construye botones
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        // IMPORTANTE: super.init() usa getImageWidth() para calcular leftPos/topPos.
        // Como sobreescribimos el getter, leftPos ya considerará el ancho total.
        super.init();

        int px   = getLeftPos() + VANILLA_W + GAP;  // inicio del panel derecho
        int py   = getTopPos();
        int listY = py + 38;
        int listX = px + 6;
        int scrollX = listX + LIST_W + 2;

        // — Scroll —
        btnScrollUp   = addRenderableWidget(Button.builder(Component.literal("▲"), b -> doScrollUp())
                .bounds(scrollX, listY, 16, ROW_H).build());
        btnScrollDown = addRenderableWidget(Button.builder(Component.literal("▼"), b -> doScrollDown())
                .bounds(scrollX, listY + (VISIBLE_ROWS - 1) * ROW_H, 16, ROW_H).build());

        // — Filas de la lista —
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int row = i;
            btnRows[i] = addRenderableWidget(Button.builder(Component.literal(""), b -> selectRow(row))
                    .bounds(listX, listY + i * ROW_H, LIST_W, ROW_H).build());
        }

        // — Controles de nivel —
        int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;

        btnModePlus  = addRenderableWidget(Button.builder(Component.literal("➕ Nivel"), b -> toggleSub(1))
                .bounds(px + 4, ctrlY, 74, 14).build());
        btnModeMinus = addRenderableWidget(Button.builder(Component.literal("➖ Nivel"), b -> toggleSub(2))
                .bounds(px + 80, ctrlY, 74, 14).build());

        // Botones de nivel: quitamos "+1K" y "MAX", ahora son 4 botones
        int bw = 36, gap2 = 2;
        int[] upD    = {  1,   5,   10,   100};
        int[] downD  = { -1,  -5,  -10,  -100};
        String[] upL   = {"+1", "+5", "+10", "+100"};
        String[] downL = {"-1", "-5", "-10", "-100"};

        btnLvlUp   = new Button[4];
        btnLvlDown = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int d = upD[i];
            btnLvlUp[i] = addRenderableWidget(Button.builder(Component.literal(upL[i]),
                    b -> adjustLevel(d)).bounds(px + 4 + i * (bw + gap2), ctrlY, bw, 14).build());
            final int dd = downD[i];
            btnLvlDown[i] = addRenderableWidget(Button.builder(Component.literal(downL[i]),
                    b -> adjustLevel(dd)).bounds(px + 4 + i * (bw + gap2), ctrlY, bw, 14).build());
        }

        btnEnchant = addRenderableWidget(Button.builder(Component.literal("⚡ ENCHANT"), b -> doEnchant())
                .bounds(px + 8, ctrlY + 18, PANEL_W - 16, 20).build());

        refreshList();
        syncButtons();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Lógica de interacción
    // ─────────────────────────────────────────────────────────────────────────────
    private void doScrollUp()   { if (scrollOffset > 0) { scrollOffset--; syncButtons(); } }
    private void doScrollDown() { if (scrollOffset < enchants.size() - VISIBLE_ROWS) { scrollOffset++; syncButtons(); } }

    private void selectRow(int visRow) {
        int idx = scrollOffset + visRow;
        if (idx >= 0 && idx < enchants.size()) {
            selectedIndex = idx;
            selectedLevel = 1;
            subMenuMode   = 0;
            syncButtons();
        }
    }

    private void toggleSub(int mode) {
        subMenuMode = (subMenuMode == mode) ? 0 : mode;
        syncButtons();
    }

    private void adjustLevel(int delta) {
        if (selectedIndex < 0) return;
        selectedLevel = Math.max(1, Math.min(selectedLevel + delta, Config.MAX_ENCHANTMENT_LEVEL));
        subMenuMode = 0;
        syncButtons();
    }

    private void doEnchant() {
        if (selectedIndex < 0 || selectedIndex >= enchants.size()) return;
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        EnchantOption opt = enchants.get(selectedIndex);
        conn.send(new C2SEnchantPacket(
                getMenu().getBlockEntity().getBlockPos(),
                opt.id(),
                selectedLevel
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
                btnRows[i].setMessage(Component.literal(prefix + warn + opt.displayName()));
                btnRows[i].visible = true;
                btnRows[i].active  = true;
            } else {
                btnRows[i].visible = false;
            }
        }

        btnScrollUp.active   = scrollOffset > 0;
        btnScrollDown.active = scrollOffset < Math.max(0, enchants.size() - VISIBLE_ROWS);

        boolean masterVisible = hasSel && subMenuMode == 0;
        btnModePlus.visible  = masterVisible || !hasSel;
        btnModeMinus.visible = masterVisible || !hasSel;
        btnModePlus.active   = hasSel;
        btnModeMinus.active  = hasSel;

        for (int i = 0; i < 4; i++) {
            btnLvlUp[i].visible   = hasSel && subMenuMode == 1;
            btnLvlDown[i].visible = hasSel && subMenuMode == 2;
        }

        btnEnchant.active = hasSel;
        btnEnchant.setMessage(Component.literal(
                hasSel ? "⚡ ENCHANT Lv" + fmtNum(selectedLevel) : "⚡ ENCHANT"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public void containerTick() {
        super.containerTick();
        try {
            ItemStack cur = getMenu().getBlockEntity().getItem(0);
            if (!ItemStack.isSameItemSameComponents(cur, lastItem)) {
                lastItem = cur.copy();
                int prev = selectedIndex;
                refreshList();
                if (prev >= 0 && prev < enchants.size()) selectedIndex = prev;
                syncButtons();
            }
        } catch (Exception ignored) {}
    }

    private void refreshList() {
        enchants.clear(); selectedIndex = -1; selectedLevel = 1; scrollOffset = 0;
        try {
            var item = getMenu().getBlockEntity().getItem(0);
            if (item.isEmpty() || Minecraft.getInstance().level == null) return;
            var reg = Minecraft.getInstance().level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT);
            
            // Obtener encantamientos actuales del item
            ItemEnchantments currentEnchants = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            
            reg.listElements().forEach(h -> {
                boolean compat = false;
                try { compat = h.value().canEnchant(item); } catch (Exception ignored) {}
                
                // Obtener nivel actual de este encantamiento
                int currentLevel = currentEnchants.getLevel(h);
                
                enchants.add(new EnchantOption(
                        h.key().identifier(),
                        Enchantment.getFullname(h, 1).getString(),
                        h.value().getMaxLevel(), h, compat, currentLevel));
            });
            enchants.sort((a, b) -> {
                if (a.isCompatible() != b.isCompatible()) return a.isCompatible() ? -1 : 1;
                return a.displayName().compareToIgnoreCase(b.displayName());
            });
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Renderizado
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * extractBackground: dibuja el fondo de la GUI.
     * En 26.1.x AbstractContainerScreen llama este método para la textura del contenedor.
     * Lo sobreescribimos para dibujar:
     *   1. La textura del contenedor vanilla (176×166)
     *   2. El fondo sólido del panel derecho (PANEL_W×166)
     *
     * CORRECCIÓN blit: En 26.1.2 la firma es:
     *   blit(Identifier texture, int x, int y, int width, int height,
     *        float u0, float v0, float u1, float v1)
     * donde u0/v0/u1/v1 son coordenadas UV NORMALIZADAS (0.0 a 1.0).
     * u0=0, v0=0 es la esquina superior izquierda de la textura.
     * u1=VANILLA_W/TEX_W, v1=VANILLA_H/TEX_H es la esquina inferior derecha del sprite.
     */
    //@Override
    protected void extractBackground(GuiGraphicsExtractor graphics,
                                     float partialTick, int mouseX, int mouseY) {
        int x = getLeftPos();
        int y = getTopPos();

        // Dibujar la textura del contenedor vanilla (solo los 176×166 del PNG)
        graphics.blit(
                TEXTURE,
                x, y,               // destino: esquina superior izquierda
                VANILLA_W, VANILLA_H, // tamaño en pantalla
                0f, 0f,              // UV inicio (esquina sup-izq de la textura)
                (float) VANILLA_W / TEX_W,  // UV ancho normalizado
                (float) VANILLA_H / TEX_H   // UV alto normalizado
        );

        // Fondo del panel derecho (rectángulo sólido, sin textura)
        int px = x + VANILLA_W + GAP;
        graphics.fill(px, y, px + PANEL_W, y + VANILLA_H, 0xDD111122);
        // Línea dorada superior del panel
        graphics.fill(px, y, px + PANEL_W, y + 2, 0xFFFFAA00);
    }

    /**
     * extractRenderState: dibuja el contenido dinámico (textos, info).
     *
     * CORRECCIÓN VELO NEGRO:
     *   ❌ ANTES: graphics.fill(0, 0, this.width, this.height, 0xDD000000)
     *      → Pintaba un rectángulo negro sobre TODA la ventana del juego.
     *   ✅ AHORA: solo dibujamos textos dentro del área del panel.
     *      El fondo ya está en extractBackground. No tocamos coordenadas absolutas.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        // 1. Llamada al super: dibuja slots, ítems, etiquetas vanilla
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 2. Contenido del panel derecho
        int px = getLeftPos() + VANILLA_W + GAP;
        int py = getTopPos();

        graphics.text(this.font, "⚡ Arcane Forge ⚡", px + 8, py + 6, 0xFFFFAA00);

        // Datos sincronizados desde el servidor
        refreshClientData();
        graphics.text(this.font,
                "Cofres: " + displayedChests + "/" + Config.MAX_LINKED_CHESTS,
                px + 8, py + 17, 0xFFAAFFAA);
        graphics.text(this.font,
                "Librerías: " + displayedBookshelves,
                px + 8, py + 27, 0xFF8888FF);

        // Fondo de la lista
        int listY = py + 38;
        graphics.fill(px + 4, listY - 2,
                px + PANEL_W - 4, listY + VISIBLE_ROWS * ROW_H + 2,
                0xBB000022);

        // Info de costo (solo si hay selección)
        if (selectedIndex >= 0 && selectedIndex < enchants.size()) {
            int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;
            EnchantOption opt = enchants.get(selectedIndex);
            int baseCost = Config.calculateEnchantCost(opt.currentLevel(), selectedLevel, displayedBookshelves);
            int cost = Config.calculateEnchantCostWithPedestal(baseCost, hasActivePedestal, selectedLevel);
            boolean canAfford = displayedMagicFuel >= cost;

            graphics.text(this.font,
                    "Costo: " + fmtNum(cost),
                    px + 8, ctrlY + 42,
                    canAfford ? 0xFF55FF55 : 0xFFFF5555);
            graphics.text(this.font,
                    "Fuel: §a" + fmtNum(displayedMagicFuel),
                    px + 8, ctrlY + 54, 0xFFFFFFFF);
            
            // Mostrar estado del pedestal
            if (hasActivePedestal) {
                graphics.text(this.font,
                        "✓ Pedestal Activo",
                        px + 8, ctrlY + 65, 0xFF55FFFF);
            } else {
                graphics.text(this.font,
                        "✗ Sin Pedestal",
                        px + 8, ctrlY + 65, 0xFFFF5555);
            }
            
            if (!enchants.get(selectedIndex).isCompatible()) {
                graphics.text(this.font,
                        "⚠ Incompatible vanilla",
                        px + 8, ctrlY + 76, 0xFFFF8800);
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics,
                                int mouseX, int mouseY, float partialTick) {
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
            }
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Scroll con rueda del ratón
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (enchants.size() > VISIBLE_ROWS) {
            if (sy > 0) doScrollUp();
            else if (sy < 0) doScrollDown();
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    private static String fmtNum(int n) {
        return n < 1000 ? String.valueOf(n) : String.format("%,d", n);
    }

    private record EnchantOption(
            Identifier id,
            String displayName,
            int vanillaMaxLevel,
            Holder<Enchantment> holder,
            boolean isCompatible,
            int currentLevel
    ) {}
}