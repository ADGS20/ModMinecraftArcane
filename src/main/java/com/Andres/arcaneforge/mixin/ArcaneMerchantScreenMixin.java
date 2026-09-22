package com.Andres.arcaneforge.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reviste visualmente el trade del Aldeano Arcano y del Aldeano Minero con
 * el mismo estilo mistico (panel azul oscuro semitransparente) del resto
 * del mod, en vez de la textura de madera/pergamino vanilla.
 *
 * No se puede reemplazar MerchantScreen registrando una pantalla nueva para
 * MenuType.MERCHANT (NeoForge no deja registrar dos veces la pantalla de un
 * mismo MenuType, y vanilla ya registro la suya) — por eso, en vez de eso,
 * se parchan directamente extractBackground() y extractLabels() de la clase
 * vanilla. Si el titulo del trade es el nuestro (se lo pusimos en
 * ArcaneNitwitTradeMixin al aldeano), se cancela el dibujo vanilla y se
 * dibuja el panel mistico encima; para cualquier otro aldeano o mercader
 * errante no se cancela nada, asi que se ve y funciona exactamente igual
 * que siempre.
 *
 * title/font/imageWidth/etc. estan declarados en Screen/AbstractContainerScreen
 * (superclases de MerchantScreen), no en MerchantScreen mismo, asi que no se
 * pueden leer con @Shadow directo aqui (@Shadow solo encuentra campos de la
 * clase objetivo exacta) — se leen a traves de ScreenAccessor /
 * AbstractContainerScreenAccessor en su lugar.
 */
@Mixin(MerchantScreen.class)
public abstract class ArcaneMerchantScreenMixin {

    @Unique private static final int AF_C_PANEL     = 0xF0140A30;
    @Unique private static final int AF_C_LIGHT     = 0xAA2D5AA8;
    @Unique private static final int AF_C_DARK      = 0xCC000A1F;
    @Unique private static final int AF_C_SLOT      = 0xAA152A52;
    @Unique private static final int AF_C_SLOT_HOLE = 0xCC000D26;

    @Unique
    private Component arcaneforge$title() {
        return ((ScreenAccessor) (Object) this).arcaneforge$title();
    }

    @Unique
    private boolean arcaneforge$isOurs() {
        String title = arcaneforge$title().getString();
        return title.startsWith("Aldeano Arcano") || title.startsWith("Aldeano Minero");
    }

    @Inject(
            method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcaneforge$mysticBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!arcaneforge$isOurs()) return;

        AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) (Object) this;
        int xo = acs.arcaneforge$leftPos();
        int yo = acs.arcaneforge$topPos();
        int imageWidth = acs.arcaneforge$imageWidth();
        int imageHeight = acs.arcaneforge$imageHeight();

        arcaneforge$beveledPanel(graphics, xo, yo, imageWidth, imageHeight);
        graphics.fill(xo, yo, xo + imageWidth, yo + 2, 0xFFAA66FF);

        // Fondo de la columna de tratos (7 filas de botones a la izquierda).
        graphics.fill(xo + 4, yo + 16, xo + 97, yo + 16 + 7 * 20 + 2, 0xEE000022);

        // Huecos de los 3 slots reales de la oferta seleccionada.
        arcaneforge$drawSlotHole(graphics, xo + 136, yo + 37);
        arcaneforge$drawSlotHole(graphics, xo + 162, yo + 37);
        arcaneforge$drawSlotHole(graphics, xo + 220, yo + 37);

        // Inventario del jugador (3x9 + hotbar), mismas coordenadas que MerchantMenu.
        arcaneforge$drawSlots(graphics, xo + 108, yo + 84, 9, 3);
        arcaneforge$drawSlots(graphics, xo + 108, yo + 142, 9, 1);

        ci.cancel();
    }

    @Inject(
            method = "extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcaneforge$mysticLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!arcaneforge$isOurs()) return;

        ScreenAccessor screen = (ScreenAccessor) (Object) this;
        AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) (Object) this;
        Font font = screen.arcaneforge$font();
        Component title = screen.arcaneforge$title();
        int imageWidth = acs.arcaneforge$imageWidth();

        // Un solo titulo centrado con todo el nombre + confianza; a proposito
        // no se usa la logica vanilla de "titulo + nivel de comercio" (esa
        // competia por espacio con la etiqueta "Comercios" y con un nombre
        // largo como el nuestro se encimaban y quedaban ilegibles).
        int totalWidth = font.width(title);
        int startX = Math.max(4, imageWidth / 2 - totalWidth / 2);
        graphics.text(font, title, startX, 6, 0xFFE0C8FF, false);

        graphics.text(font, acs.arcaneforge$playerInventoryTitle(), acs.arcaneforge$inventoryLabelX(), acs.arcaneforge$inventoryLabelY(), 0xFFAAAAAA, false);

        ci.cancel();
    }

    @Unique
    private void arcaneforge$beveledPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, AF_C_PANEL);
        g.fill(x, y, x + w, y + 1, AF_C_LIGHT);
        g.fill(x, y, x + 1, y + h, AF_C_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, AF_C_DARK);
        g.fill(x + w - 1, y, x + w, y + h, AF_C_DARK);
    }

    @Unique
    private void arcaneforge$drawSlotHole(GuiGraphicsExtractor g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, AF_C_SLOT);
        g.fill(sx, sy, sx + 16, sy + 16, AF_C_SLOT_HOLE);
    }

    @Unique
    private void arcaneforge$drawSlots(GuiGraphicsExtractor g, int gx, int gy, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                arcaneforge$drawSlotHole(g, gx + col * 18, gy + row * 18);
            }
        }
    }
}
