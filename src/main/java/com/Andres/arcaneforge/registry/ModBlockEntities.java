package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.block.ArcaneGolemGeneratorBlockEntity;
import com.Andres.arcaneforge.block.ArcanePowerBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneGolemGeneratorBlockEntity>> GOLEM_GENERATOR_BE =
            BLOCK_ENTITIES.register(
                    "arcane_golem_generator",
                    () -> new BlockEntityType<>(
                            ArcaneGolemGeneratorBlockEntity::new,
                            Set.of(ModBlocks.ARCANE_GOLEM_GENERATOR.get()),
                            false
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcanePowerBlockEntity>> ARCANE_POWER_BLOCK_BE =
            BLOCK_ENTITIES.register(
                    "arcane_power_block",
                    () -> new BlockEntityType<>(
                            ArcanePowerBlockEntity::new,
                            Set.of(ModBlocks.ARCANE_POWER_BLOCK.get()),
                            false
                    )
            );
}
