package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> Servidor: el jugador pulso el boton de activar/desactivar el iman de la bolsa. */
public record C2SToggleMagnetPacket() implements CustomPacketPayload {

    public static final Type<C2SToggleMagnetPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "toggle_magnet"));

    public void write(FriendlyByteBuf buf) {}

    public static C2SToggleMagnetPacket read(FriendlyByteBuf buf) {
        return new C2SToggleMagnetPacket();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SToggleMagnetPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;
            if (serverPlayer.containerMenu instanceof DimensionalBagMenu menu) {
                menu.toggleMagnet(serverPlayer);
            }
        });
    }
}
