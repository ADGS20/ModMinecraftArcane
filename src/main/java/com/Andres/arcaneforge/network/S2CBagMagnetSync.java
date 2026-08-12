package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor -> Cliente: confirma el nuevo estado (on/off) del iman de la bolsa. */
public record S2CBagMagnetSync(boolean enabled) implements CustomPacketPayload {

    public static final Type<S2CBagMagnetSync> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "bag_magnet_sync"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static S2CBagMagnetSync read(FriendlyByteBuf buf) {
        return new S2CBagMagnetSync(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CBagMagnetSync pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof DimensionalBagMenu menu) {
                menu.setMagnetEnabledClient(pkt.enabled());
            }
        });
    }
}
