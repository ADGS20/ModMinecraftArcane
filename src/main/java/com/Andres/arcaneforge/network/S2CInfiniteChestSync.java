package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.util.IArcaneInfiniteChest;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor -> Cliente: le dice al cliente que el ChestMenu que acaba de
 * abrir (identificado por containerId) corresponde a un Cofre Infinito, asi
 * el cliente puede marcar su propio SimpleContainer (la copia local que usa
 * solo para dibujar la pantalla, ver SimpleContainerCapacityMixin) y dejar
 * de recortar los stacks recibidos por sync a 64/99.
 */
public record S2CInfiniteChestSync(int containerId) implements CustomPacketPayload {

    public static final Type<S2CInfiniteChestSync> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "infinite_chest_sync"));

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
    }

    public static S2CInfiniteChestSync read(FriendlyByteBuf buf) {
        return new S2CInfiniteChestSync(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CInfiniteChestSync pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null
                    && mc.player.containerMenu.containerId == pkt.containerId()
                    && mc.player.containerMenu instanceof ChestMenu chestMenu
                    && chestMenu.getContainer() instanceof IArcaneInfiniteChest chest) {
                chest.arcaneforge$setInfinite(true);
            }
        });
    }
}
