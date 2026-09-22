package com.Andres.arcaneforge.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Ver ScreenAccessor: mismo motivo, pero para los campos que declara AbstractContainerScreen. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("imageWidth")
    int arcaneforge$imageWidth();

    @Accessor("imageHeight")
    int arcaneforge$imageHeight();

    @Accessor("leftPos")
    int arcaneforge$leftPos();

    @Accessor("topPos")
    int arcaneforge$topPos();

    @Accessor("playerInventoryTitle")
    Component arcaneforge$playerInventoryTitle();

    @Accessor("inventoryLabelX")
    int arcaneforge$inventoryLabelX();

    @Accessor("inventoryLabelY")
    int arcaneforge$inventoryLabelY();
}
