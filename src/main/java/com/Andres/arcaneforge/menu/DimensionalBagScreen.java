package com.Andres.arcaneforge.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Pantalla del Saco Dimensional. Dibuja un fondo propio con el ESTILO de las GUI
 * de Minecraft (panel gris claro biselado + slots hundidos), bien visible. Las
 * PESTANAS de pagina (arriba/abajo) van a la derecha en su propio panel.
 */
public class DimensionalBagScreen extends AbstractContainerScreen<DimensionalBagMenu> {

    private static final int GUI_W = 176;
    private static final int GUI_H = 222;

    // Coordenadas de los slots (coinciden con DimensionalBagMenu.layoutSlots)
    private static final int STORE_X = 8;
    private static final int STORE_Y = 18;
    private static final int INV_Y = 18 + 6 * 18 + 13; // 139

    // Paleta estilo GUI vanilla
    private static final int C_PANEL      = 0xFFC6C6C6; // gris claro del fondo
    private static final int C_LIGHT      = 0xFFFFFFFF; // bisel claro
    private static final int C_DARK       = 0xFF555555; // bisel oscuro
    private static final int C_SLOT       = 0xFF8B8B8B; // borde de slot
    private static final int C_SLOT_HOLE  = 0xFF373737; // hueco del slot

    public DimensionalBagScreen(DimensionalBagMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public int getImageWidth() {
        return GUI_W;
    }

    @Override
    public int getImageHeight() {
        return GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;

        // Etiquetas de texto por defecto: las ponemos donde no estorben.
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = STORE_X;
        this.inventoryLabelY = INV_Y - 12;

        // Sin botones de pagina: el saco es siempre 1 pagina de almacenamiento masivo.
    }

    // Dibuja un rectangulo con bisel estilo vanilla (claro arriba/izq, oscuro abajo/der).
    private void beveledPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, C_PANEL);
        g.fill(x, y, x + w, y + 1, C_LIGHT);          // arriba
        g.fill(x, y, x + 1, y + h, C_LIGHT);          // izquierda
        g.fill(x, y + h - 1, x + w, y + h, C_DARK);   // abajo
        g.fill(x + w - 1, y, x + w, y + h, C_DARK);   // derecha
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = getLeftPos();
        int y = getTopPos();

        // Panel principal estilo vanilla
        beveledPanel(graphics, x, y, GUI_W, GUI_H);

        // Rejilla de almacen (6x9 = 54)
        drawSlots(graphics, x + STORE_X, y + STORE_Y, 9, 6);
        // Inventario jugador (3 filas) + hotbar (1 fila)
        drawSlots(graphics, x + STORE_X, y + INV_Y, 9, 3);
        drawSlots(graphics, x + STORE_X, y + INV_Y + 58, 9, 1);

        // Sin panel de pestanas: una sola pagina de almacenamiento masivo.
    }

    private void drawSlots(GuiGraphicsExtractor graphics, int gx, int gy, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = gx + col * 18;
                int sy = gy + row * 18;
                // marco claro + hueco hundido oscuro (estilo cofre vanilla)
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, C_SLOT);
                graphics.fill(sx, sy, sx + 16, sy + 16, C_SLOT_HOLE);
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, partialTick, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Los slots de la bolsa aceptan hasta 1 000 000 000 (1B) por slot; mostrar
     * ese numero completo no cabe en el icono. Debajo de 1000 se muestra el
     * numero tal cual (hasta 3 digitos: 999). Desde 1000 se abrevia con letra
     * (K = mil, M = millon, B = billon/1000M) y siempre con maximo 3 digitos
     * antes de la letra: "1K"… "999K", "1M"… "999M", "1B" (el tope exacto).
     * Sin decimales.
     */
    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, String countString) {
        String label = stack.getCount() >= 1000 ? formatCompact(stack.getCount()) : countString;
        super.renderSlotContents(graphics, stack, slot, label);
    }

    private static String formatCompact(int count) {
        if (count < 1_000) return String.valueOf(count);
        if (count < 1_000_000) return (count / 1_000) + "K";
        if (count < 1_000_000_000) return (count / 1_000_000) + "M";
        return (count / 1_000_000_000) + "B";
    }
}
