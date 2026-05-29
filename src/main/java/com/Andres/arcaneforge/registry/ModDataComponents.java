package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.component.ArcaneEnchantments;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * ModDataComponents — Registro del Data Component ArcaneEnchantments.
 *
 * CORRECCIÓN 26.1.2:
 *   DeferredRegister.createDataComponents(String)  ← NO EXISTE en 26.1
 *   DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, String) ← CORRECTO
 */
public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRAR =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArcaneForge.MODID);

    public static final Supplier<DataComponentType<ArcaneEnchantments>> ARCANE_ENCHANTMENTS =
            REGISTRAR.register(
                    "arcane_enchantments",
                    () -> DataComponentType.<ArcaneEnchantments>builder()
                            .persistent(ArcaneEnchantments.CODEC)
                            .networkSynchronized(ArcaneEnchantments.STREAM_CODEC)
                            .build()
            );
}