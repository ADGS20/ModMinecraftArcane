package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.menu.DimensionalBagMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor -> Cliente: actualiza el numero de pagina mostrado en el saco. */
public record S2CBagPageSync(int currentPage, int totalPages) implements CustomPacketPayload {

    public static final Type<S2CBagPageSync> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "bag_page_sync"));

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(currentPage);
        buf.writeVarInt(totalPages);
    }

    public static S2CBagPageSync read(FriendlyByteBuf buf) {
        int cp = buf.readVarInt();
        int tp = buf.readVarInt();
        return new S2CBagPageSync(cp, tp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CBagPageSync pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof DimensionalBagMenu menu) {
                menu.setCurrentPageClient(pkt.currentPage());
            }
        });
    }
}
