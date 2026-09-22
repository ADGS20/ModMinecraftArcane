package com.Andres.arcaneforge.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @Shadow no encuentra campos heredados de una SUPERCLASE del target (solo
 * los declarados directamente en la clase objetivo), asi que para leer
 * title/font de Screen desde un mixin que apunta a MerchantScreen hace falta
 * este Accessor Mixin aparte, apuntando a la clase que de verdad los declara.
 */
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("title")
    Component arcaneforge$title();

    @Accessor("font")
    Font arcaneforge$font();
}
