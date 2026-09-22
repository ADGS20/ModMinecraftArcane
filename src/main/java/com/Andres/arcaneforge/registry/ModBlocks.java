package com.Andres.arcaneforge.registry;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneDiscountBlock;
import com.Andres.arcaneforge.block.ArcaneForgeBlock;
import com.Andres.arcaneforge.block.ArcaneGolemGeneratorBlock;
import com.Andres.arcaneforge.block.ArcanePedestalBlock;
import com.Andres.arcaneforge.block.ArcanePowerBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneForge.MODID);

    // El bloque del ArcaneForge — usa ArcaneForgeBlock para tener GUI y BlockEntity
    public static final DeferredBlock<ArcaneForgeBlock> ARCANE_FORGE = BLOCKS.register(
            "arcane_forge",
            () -> new ArcaneForgeBlock(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge"))))
    );

    // El pedestal — usa ArcanePedestalBlock para su lógica de detección
    public static final DeferredBlock<ArcanePedestalBlock> ARCANE_PEDESTAL = BLOCKS.register(
            "arcane_pedestal",
            () -> new ArcanePedestalBlock(BlockBehaviour.Properties.of()
                    .destroyTime(2.0f)
                    .explosionResistance(5.0f)
                    .lightLevel(state -> 7) // luz propia, mas tenue que una antorcha (14)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_pedestal"))))
    );

    // El generador de golems — usa ArcaneGolemGeneratorBlock para su propio BlockEntity
    public static final DeferredBlock<ArcaneGolemGeneratorBlock> ARCANE_GOLEM_GENERATOR = BLOCKS.register(
            "arcane_golem_generator",
            () -> new ArcaneGolemGeneratorBlock(BlockBehaviour.Properties.of()
                    .destroyTime(4.0f)
                    .explosionResistance(8.0f)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_golem_generator"))))
    );

    // El bloque de poder arcano — bloque genérico decorativo
    public static final DeferredBlock<ArcanePowerBlock> ARCANE_POWER_BLOCK = BLOCKS.register(
            "arcane_power_block",
            () -> new ArcanePowerBlock(BlockBehaviour.Properties.of()
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .lightLevel(state -> 15) // el nucleo de poder brilla al maximo
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_power_block"))))
    );

    // BlockItems — necesarios para que los bloques existan como items en el inventario
    public static final DeferredItem<BlockItem> ARCANE_FORGE_ITEM =
            ModItems.ITEMS.register("arcane_forge",
                    () -> new BlockItem(ARCANE_FORGE.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_forge")))));

    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_ITEM =
            ModItems.ITEMS.register("arcane_pedestal",
                    () -> new BlockItem(ARCANE_PEDESTAL.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_pedestal")))));

    public static final DeferredItem<BlockItem> ARCANE_POWER_BLOCK_ITEM =
            ModItems.ITEMS.register("arcane_power_block",
                    () -> new BlockItem(ARCANE_POWER_BLOCK.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_power_block")))));

    public static final DeferredItem<BlockItem> ARCANE_GOLEM_GENERATOR_ITEM =
            ModItems.ITEMS.register("arcane_golem_generator",
                    () -> new BlockItem(ARCANE_GOLEM_GENERATOR.get(),
                            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "arcane_golem_generator")))));

    // Bloques de descuento que entrega el aldeano verde (Nitwit) a cambio de esmeraldas + item.
    // Rama MATERIAL (visual = Pedestal): abarata el coste de magic fuel de la Forja.
    // Rama XP (visual = Bloque de Poder): abarata el coste de niveles de experiencia de la Forja.
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_PEDESTAL_DISCOUNT_1 = registerDiscountBlock("arcane_pedestal_discount_1", ArcaneDiscountBlock.Branch.MATERIAL, 1, 0.10f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_PEDESTAL_DISCOUNT_2 = registerDiscountBlock("arcane_pedestal_discount_2", ArcaneDiscountBlock.Branch.MATERIAL, 2, 0.20f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_PEDESTAL_DISCOUNT_3 = registerDiscountBlock("arcane_pedestal_discount_3", ArcaneDiscountBlock.Branch.MATERIAL, 3, 0.35f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_POWER_DISCOUNT_1 = registerDiscountBlock("arcane_power_discount_1", ArcaneDiscountBlock.Branch.XP, 1, 0.10f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_POWER_DISCOUNT_2 = registerDiscountBlock("arcane_power_discount_2", ArcaneDiscountBlock.Branch.XP, 2, 0.20f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_POWER_DISCOUNT_3 = registerDiscountBlock("arcane_power_discount_3", ArcaneDiscountBlock.Branch.XP, 3, 0.35f);

    // Nivel 4 ("Confianza Máxima"): solo se desbloquea tras comprar el Nivel 3
    // de AMBAS ramas. Se paga con Tótem de la Inmortalidad + otro ingrediente
    // caro (nada de esmeraldas), y da 50% de descuento.
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_PEDESTAL_DISCOUNT_4 = registerDiscountBlock("arcane_pedestal_discount_4", ArcaneDiscountBlock.Branch.MATERIAL, 4, 0.50f);
    public static final DeferredBlock<ArcaneDiscountBlock> ARCANE_POWER_DISCOUNT_4 = registerDiscountBlock("arcane_power_discount_4", ArcaneDiscountBlock.Branch.XP, 4, 0.50f);

    private static DeferredBlock<ArcaneDiscountBlock> registerDiscountBlock(String name, ArcaneDiscountBlock.Branch branch, int tier, float discount) {
        return BLOCKS.register(name,
                () -> new ArcaneDiscountBlock(BlockBehaviour.Properties.of()
                        .destroyTime(2.0f)
                        .explosionResistance(5.0f)
                        .setId(ResourceKey.create(Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(ArcaneForge.MODID, name))),
                        branch, tier, discount));
    }

    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_DISCOUNT_1_ITEM = registerDiscountItem("arcane_pedestal_discount_1", ARCANE_PEDESTAL_DISCOUNT_1);
    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_DISCOUNT_2_ITEM = registerDiscountItem("arcane_pedestal_discount_2", ARCANE_PEDESTAL_DISCOUNT_2);
    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_DISCOUNT_3_ITEM = registerDiscountItem("arcane_pedestal_discount_3", ARCANE_PEDESTAL_DISCOUNT_3);
    public static final DeferredItem<BlockItem> ARCANE_POWER_DISCOUNT_1_ITEM = registerDiscountItem("arcane_power_discount_1", ARCANE_POWER_DISCOUNT_1);
    public static final DeferredItem<BlockItem> ARCANE_POWER_DISCOUNT_2_ITEM = registerDiscountItem("arcane_power_discount_2", ARCANE_POWER_DISCOUNT_2);
    public static final DeferredItem<BlockItem> ARCANE_POWER_DISCOUNT_3_ITEM = registerDiscountItem("arcane_power_discount_3", ARCANE_POWER_DISCOUNT_3);
    public static final DeferredItem<BlockItem> ARCANE_PEDESTAL_DISCOUNT_4_ITEM = registerDiscountItem("arcane_pedestal_discount_4", ARCANE_PEDESTAL_DISCOUNT_4);
    public static final DeferredItem<BlockItem> ARCANE_POWER_DISCOUNT_4_ITEM = registerDiscountItem("arcane_power_discount_4", ARCANE_POWER_DISCOUNT_4);

    private static DeferredItem<BlockItem> registerDiscountItem(String name, DeferredBlock<ArcaneDiscountBlock> block) {
        return ModItems.ITEMS.register(name,
                () -> new BlockItem(block.get(),
                        new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                        Identifier.fromNamespaceAndPath(ArcaneForge.MODID, name)))
                                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    }
}