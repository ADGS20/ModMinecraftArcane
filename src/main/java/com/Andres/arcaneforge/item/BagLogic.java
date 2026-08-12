package com.Andres.arcaneforge.item;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/** Logica compartida del Saco Dimensional (paginas, nivel). */
public final class BagLogic {

    public static final ResourceKey<Enchantment> DIMENSIONAL_BIND_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "dimensional_bind"));

    private BagLogic() {}

    /**
     * Siempre 1 pagina. El almacenamiento ilimitado viene de los stacks por slot
     * (hasta 1 000 000 000 unidades por slot en lugar de 64).
     */
    public static int pagesForLevel(int level) {
        return 1;
    }

    public static int getBindLevel(Player player, ItemStack bag) {
        if (bag.isEmpty()) return 0;
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<net.minecraft.core.Holder.Reference<Enchantment>> opt =
                    registry.get(DIMENSIONAL_BIND_KEY);
            if (opt.isEmpty()) return 0;
            ItemEnchantments ench = bag.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return ench.getLevel(opt.get());
        } catch (Exception e) {
            return 0;
        }
    }
}
