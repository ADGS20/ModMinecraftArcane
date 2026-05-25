package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.ArcaneForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ArcaneForge.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneForgeMenu>> ARCANE_FORGE_MENU =
            MENUS.register(
                    "arcane_forge_menu",
                    () -> IMenuTypeExtension.create(ArcaneForgeMenu::new)
            );
}
