package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArcaneForge.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneForgeBlockEntity>> ARCANE_FORGE_BE =
            BLOCK_ENTITIES.register(
                    "arcane_forge",
                    () -> new BlockEntityType<>(
                            ArcaneForgeBlockEntity::new,
                            Set.of(ModBlocks.ARCANE_FORGE.get()),
                            false
                    )
            );
}
