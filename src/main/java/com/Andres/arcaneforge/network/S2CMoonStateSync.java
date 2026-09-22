package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor -> Cliente: avisa si la Luna Oscura/Roja esta activa ahora mismo, para teñir el cielo. */
public record S2CMoonStateSync(boolean active) implements CustomPacketPayload {

    public static final Type<S2CMoonStateSync> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "moon_state_sync"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static S2CMoonStateSync read(FriendlyByteBuf buf) {
        return new S2CMoonStateSync(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMoonStateSync pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.Andres.arcaneforge.client.ClientMoonState.setActive(pkt.active()));
    }
}
