package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → Cliente: Sincroniza datos de la forja.
 */
public record S2CSyncPacket(BlockPos forgePos, int linkedChests, int bookshelves, int magicFuel)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "sync"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(forgePos);
        buf.writeVarInt(linkedChests);
        buf.writeVarInt(bookshelves);
        buf.writeVarInt(magicFuel);
    }

    public static S2CSyncPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int chests = buf.readVarInt();
        int shelves = buf.readVarInt();
        int fuel = buf.readVarInt();
        return new S2CSyncPacket(pos, chests, shelves, fuel);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null && ctx.player().level() != null) {
                BlockEntity be = ctx.player().level().getBlockEntity(pkt.forgePos());
                if (be instanceof ArcaneForgeBlockEntity forge) {
                    forge.setClientSyncData(pkt.linkedChests(), pkt.bookshelves(), pkt.magicFuel());
                }
            }
        });
    }
}
