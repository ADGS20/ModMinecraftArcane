package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> Servidor: el jugador pidio cambiar a otra pagina del saco. */
public record C2SBagPagePacket(int direction) implements CustomPacketPayload {

    public static final Type<C2SBagPagePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "bag_page"));

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(direction);
    }

    public static C2SBagPagePacket read(FriendlyByteBuf buf) {
        return new C2SBagPagePacket(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SBagPagePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;
            if (serverPlayer.containerMenu instanceof DimensionalBagMenu menu) {
                menu.changePage(serverPlayer, pkt.direction());
            }
        });
    }
}
