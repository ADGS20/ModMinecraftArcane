package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.miners.OreFilter;
import com.Andres.arcaneforge.network.C2SMinersSightSettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu de configuracion de la Vision Minera del casco. No es un contenedor
 * (no hay slots que sincronizar): se abre localmente en el cliente leyendo
 * el estado guardado en el casco puesto, y cada boton manda de inmediato el
 * nuevo estado al servidor via C2SMinersSightSettingsPacket, igual que hace
 * el boton on/off del Saco Dimensional.
 */
public class MinersSightScreen extends Screen {

    private static final OreFilter[] FILTERS = OreFilter.values();
    private static final int COLS = 3;
    private static final int BTN_W = 92;
    private static final int BTN_H = 20;
    private static final int GAP = 4;

    private boolean enabled;
    private OreFilter filter;

    private Button btnToggle;
    private final Button[] btnFilters = new Button[FILTERS.length];

    public MinersSightScreen(boolean enabled, OreFilter filter) {
        super(Component.literal("Vision Minera"));
        this.enabled = enabled;
        this.filter = filter;
    }

    @Override
    protected void init() {
        super.init();

        int rows = (int) Math.ceil(FILTERS.length / (double) COLS);
        int gridW = COLS * BTN_W + (COLS - 1) * GAP;
        int gridH = rows * BTN_H + (rows - 1) * GAP;

        int startX = (this.width - gridW) / 2;
        int startY = (this.height - gridH) / 2;

        btnToggle = addRenderableWidget(Button.builder(toggleLabel(), b -> onToggle())
                .bounds(startX, startY - BTN_H - 10, gridW, BTN_H).build());

        for (int i = 0; i < FILTERS.length; i++) {
            int row = i / COLS;
            int col = i % COLS;
            final OreFilter f = FILTERS[i];
            btnFilters[i] = addRenderableWidget(Button.builder(Component.literal(f.displayName()), b -> onSelectFilter(f))
                    .bounds(startX + col * (BTN_W + GAP), startY + row * (BTN_H + GAP), BTN_W, BTN_H).build());
        }

        refreshHighlight();
    }

    private Component toggleLabel() {
        return Component.literal(enabled ? "§aVision Minera: ON" : "§cVision Minera: OFF");
    }

    private void onToggle() {
        enabled = !enabled;
        btnToggle.setMessage(toggleLabel());
        sendSettings();
    }

    private void onSelectFilter(OreFilter f) {
        filter = f;
        refreshHighlight();
        sendSettings();
    }

    private void refreshHighlight() {
        for (int i = 0; i < FILTERS.length; i++) {
            boolean sel = FILTERS[i] == filter;
            btnFilters[i].setMessage(Component.literal((sel ? "§e► " : "") + FILTERS[i].displayName()));
        }
    }

    private void sendSettings() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        conn.send(new C2SMinersSightSettingsPacket(enabled, filter.id()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String title = "⚡ Vision Minera ⚡";
        graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2, 20, 0xFFFFAA00);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
