package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → Cliente: Resultado de un intento de encantamiento.
 */
public record S2CResultPacket(boolean success, String message)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CResultPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "result"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUtf(message, 512);
    }

    public static S2CResultPacket read(FriendlyByteBuf buf) {
        boolean ok = buf.readBoolean();
        String msg = buf.readUtf(512);
        return new S2CResultPacket(ok, msg);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CResultPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null) {
                // sendSystemMessage reemplaza displayClientMessage en 26.1.2
                ctx.player().sendSystemMessage(Component.literal(pkt.message()));
            }
        });
    }
}
