package com.Andres.arcaneforge.menu;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
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
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArcaneForgeScreen extends AbstractContainerScreen<ArcaneForgeMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            ArcaneForge.MOD_ID, "textures/gui/container/arcane_forge.png");

    private static final float TEX_W = 256f;
    private static final float TEX_H = 256f;

    private static final int VANILLA_W  = 176;
    private static final int VANILLA_H  = 166;
    private static final int GAP        = 4;
    private static final int PANEL_W    = 156;
    private static final int TOTAL_W    = VANILLA_W + GAP + PANEL_W;

    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_H        = 14;
    private static final int LIST_W       = 124;

    private final List<EnchantOption> enchants = new ArrayList<>();
    private int selectedIndex  = -1;
    private int selectedLevel  = 1;
    private int scrollOffset   = 0;
    private int subMenuMode    = 0;

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
        return VANILLA_H;
    }

    @Override
    protected void init() {
        super.init();

        int px   = getLeftPos() + VANILLA_W + GAP;
        int py   = getTopPos();
        int listY = py + 38;
        int listX = px + 6;
        int scrollX = listX + LIST_W + 2;

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
        int[] downD  = { -1,  -5,  -10,   0};
        String[] upL   = {"+1", "+5", "+10", "Max"};
        String[] downL = {"-1", "-5", "-10", "Reset"};

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
        EnchantOption opt = enchants.get(selectedIndex);
        int currentLevel = opt.currentLevel();

        boolean isCreative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
        int maxLimit = hasActivePedestal ? 1000 : 255;

        if (delta == 9999) {
            if (isCreative) {
                selectedLevel = maxLimit - currentLevel;
            } else {
                float enchMult = ArcaneForgeBlockEntity.getEnchantmentMultiplier(opt.id());
                int maxPossibleToAdd = 0;
                int fuel = displayedMagicFuel;
                int testLevel = currentLevel;

                while (testLevel < maxLimit) {
                    int baseCostForOne = ArcaneForgeBlockEntity.calculateProgressiveCost(
                            testLevel, 1, displayedBookshelves, hasActivePedestal);
                    int realCostForOne = Math.max(1, Math.round(baseCostForOne * enchMult));
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
                hasSel ? "⚡ ENCHANT +" + fmtNum(selectedLevel) : "⚡ ENCHANT"));
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
            var item = getMenu().getSlot(0).getItem();
            if (item.isEmpty() || Minecraft.getInstance().level == null) return;

            var reg = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemEnchantments currentEnchants = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            boolean isTotem  = item.is(Items.TOTEM_OF_UNDYING);
            boolean isRanged = item.is(Items.BOW) || item.is(Items.CROSSBOW);

            reg.listElements().forEach(h -> {
                try {
                    if (!h.isBound()) return;

                    net.minecraft.resources.Identifier id = h.key().identifier();
                    boolean isOurTotemEnchant = id.getNamespace().equals(ArcaneForge.MODID) && id.getPath().equals("void_protection");
                    boolean isApocalyptic     = id.getNamespace().equals(ArcaneForge.MODID) && id.getPath().equals("apocalyptic_judgment");

                    if (isTotem  && !isOurTotemEnchant) return;
                    if (!isTotem && isOurTotemEnchant)  return;
                    if (isApocalyptic && !isRanged)     return;

                    boolean vanillaCompat = false;
                    try { vanillaCompat = h.value().canEnchant(item); }
                    catch (Exception ignored) {}

                    boolean finalCompat = vanillaCompat || (hasActivePedestal && (isTotem || !vanillaCompat));
                    if (isApocalyptic) finalCompat = hasActivePedestal;

                    int currentLevel = currentEnchants.getLevel(h);
                    enchants.add(new EnchantOption(
                            id,
                            Enchantment.getFullname(h, 1).getString(),
                            h.value().getMaxLevel(), h, finalCompat, currentLevel));
                } catch (Exception e) {}
            });

            enchants.sort((a, b) -> {
                boolean aCustom = a.id().getNamespace().equals(ArcaneForge.MODID);
                boolean bCustom = b.id().getNamespace().equals(ArcaneForge.MODID);
                if (aCustom != bCustom) return aCustom ? -1 : 1;
                if (a.isCompatible() != b.isCompatible()) return a.isCompatible() ? -1 : 1;
                return a.displayName().compareToIgnoreCase(b.displayName());
            });
        } catch (Exception e) {
            ArcaneForge.LOGGER.error("Error al refrescar la lista de la forja: ", e);
        }
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = getLeftPos();
        int y = getTopPos();

        graphics.blit(TEXTURE, x, y, VANILLA_W, VANILLA_H, 0f, 0f, (float) VANILLA_W / TEX_W, (float) VANILLA_H / TEX_H);

        int px = x + VANILLA_W + GAP;
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
            int prev = selectedIndex;
            refreshList();
            if (prev >= 0 && prev < enchants.size()) selectedIndex = prev;
            syncButtons();
        }

        graphics.text(this.font, "Cofres: " + displayedChests + "/" + Config.MAX_LINKED_CHESTS, px + 8, py + 17, 0xFFAAFFAA);
        graphics.text(this.font, "Librerías: " + displayedBookshelves, px + 8, py + 27, 0xFF8888FF);

        int listY = py + 38;
        graphics.fill(px + 4, listY - 2, px + PANEL_W - 4, listY + VISIBLE_ROWS * ROW_H + 2, 0xBB000022);

        if (selectedIndex >= 0 && selectedIndex < enchants.size()) {
            int ctrlY = listY + VISIBLE_ROWS * ROW_H + 6;
            EnchantOption opt = enchants.get(selectedIndex);

            int baseCost  = ArcaneForgeBlockEntity.calculateProgressiveCost(opt.currentLevel(), selectedLevel, displayedBookshelves, hasActivePedestal);
            float enchMult = ArcaneForgeBlockEntity.getEnchantmentMultiplier(opt.id());
            int totalCost  = Math.max(1, Math.round(baseCost * enchMult));

            boolean isCreative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
            if (isCreative) totalCost = 0;

            boolean canAfford = isCreative || (displayedMagicFuel >= totalCost);

            graphics.text(this.font, "Fuel material: " + fmtNum(totalCost), px + 8, ctrlY + 36, canAfford ? 0xFF55FF55 : 0xFFFF5555);

            if (!isCreative && Minecraft.getInstance().player != null) {
                int xpCost = Math.max(1, (int)(selectedLevel * 3 * enchMult));
                int playerXP = Minecraft.getInstance().player.experienceLevel;
                boolean canAffordXP = playerXP >= xpCost;
                graphics.text(this.font, "EXP: -" + xpCost + " lvl (tienes " + playerXP + ")", px + 8, ctrlY + 46, canAffordXP ? 0xFFFFFF55 : 0xFFFF5555);
            } else if (isCreative) {
                graphics.text(this.font, "EXP: Gratis (Creativo)", px + 8, ctrlY + 46, 0xFF55FF55);
            }

            String multStr = enchMult == 1.0f ? "x1 (Común)" : enchMult == 2.5f ? "x2.5 (Raro vanilla)" : enchMult == 3.0f ? "x3 (Mod Arcano)" : "x5 (LEGENDARIO)";
            int multColor  = enchMult >= 5.0f ? 0xFFFF00FF : enchMult >= 3.0f ? 0xFF8800FF : enchMult >= 2.5f ? 0xFF00FFFF : 0xFFFFFFFF;
            graphics.text(this.font, "Rareza: " + multStr, px + 8, ctrlY + 56, multColor);

            int yOff = ctrlY + 66;
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
                graphics.text(this.font, "✓ Pedestal Activo (Max 1000)", px + 8, yOff, 0xFF55FFFF);
            } else {
                graphics.text(this.font, "✗ Sin Pedestal (Max 255)", px + 8, yOff, 0xFFFF5555);
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

    private record EnchantOption(Identifier id, String displayName, int vanillaMaxLevel, Holder<Enchantment> holder, boolean isCompatible, int currentLevel) {}
}