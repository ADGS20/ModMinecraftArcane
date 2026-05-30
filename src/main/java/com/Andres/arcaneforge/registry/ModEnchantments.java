package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(Registries.ENCHANTMENT, ArcaneForge.MODID);

    public static final ResourceKey<Enchantment> APOCALYPTIC_JUDGMENT_KEY = 
            ResourceKey.create(Registries.ENCHANTMENT, 
                Identifier.of(ArcaneForge.MODID, "apocalyptic_judgment"));

    public static final DeferredHolder<Enchantment, Enchantment> APOCALYPTIC_JUDGMENT = 
            ENCHANTMENTS.register("apocalyptic_judgment", () -> null);

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        
        Enchantment enchantment = Enchantment.builder(
            Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                10,
                1,
                Cost.constant(25),
                Cost.constant(50),
                2,
                EquipmentSlotGroup.MAINHAND
            )
        )
        .withEffect(EnchantmentEffectComponents.DAMAGE, 1.0f)
        .build(Identifier.of(ArcaneForge.MODID, "apocalyptic_judgment"));
        
        context.register(APOCALYPTIC_JUDGMENT_KEY, enchantment);
    }

    public static void register() {
        // Registration automática vía DeferredRegister
    }
}