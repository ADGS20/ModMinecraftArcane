package com.Andres.arcaneforge.block;

import com.Andres.arcaneforge.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Generador Arcano de Gólems — se le inserta un Núcleo ya encantado en la
 * Forja (clic derecho con el Núcleo en mano) y, mientras tenga cofres
 * vinculados con Bloques de Hierro y Calabazas Talladas de sobra, va
 * sacando gólems de hierro con exactamente los mismos encantamientos y
 * bonos que uno vinculado a mano — sin volver a gastar experiencia del
 * jugador (ver ArcaneGolemGeneratorBlockEntity.tick).
 *
 * Sin GUI propia: todo por interaccion directa, igual que el Cetro Arcano
 * del Golem o la Vara de Vinculacion.
 */
public class ArcaneGolemGeneratorBlock extends BaseEntityBlock {

    public static final MapCodec<ArcaneGolemGeneratorBlock> CODEC = simpleCodec(ArcaneGolemGeneratorBlock::new);

    public ArcaneGolemGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneGolemGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ArcaneGolemGeneratorBlockEntity be)) {
            return InteractionResult.PASS;
        }

        if (be.insertCore(itemStack, player)) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.sendSystemMessage(Component.literal(
                    "§a✔ Nucleo insertado. Vincula cofres con la Vara: 8 Hierro + 2 Calabaza Tallada"
                            + " (golem de hierro), o 4 Nieve + 2 Calabaza Tallada (golem de nieve)."));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
                                                BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ArcaneGolemGeneratorBlockEntity be)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            ItemStack ejected = be.ejectCore();
            if (!ejected.isEmpty()) {
                if (!player.addItem(ejected)) {
                    player.drop(ejected, false);
                }
                player.sendSystemMessage(Component.literal("§7Nucleo expulsado."));
                return InteractionResult.CONSUME;
            }
        }

        player.sendSystemMessage(Component.literal(
                "§7Nucleo: " + (be.hasCore() ? "§ainsertado" : "§cvacio")
                        + " §7| Cofres vinculados: §f" + be.getLinkedChestCount()));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GOLEM_GENERATOR_BE.get(), ArcaneGolemGeneratorBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
