package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.miners.MinersSightLogic;
import com.Andres.arcaneforge.miners.OreFilter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> Servidor: nuevo estado (activado/filtro) de la Vision Minera del casco puesto. */
public record C2SMinersSightSettingsPacket(boolean enabled, String filterId) implements CustomPacketPayload {

    public static final Type<C2SMinersSightSettingsPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "miners_sight_settings"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeUtf(filterId);
    }

    public static C2SMinersSightSettingsPacket read(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        String filterId = buf.readUtf();
        return new C2SMinersSightSettingsPacket(enabled, filterId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMinersSightSettingsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            ItemStack helmet = sp.getItemBySlot(EquipmentSlot.HEAD);
            int level = helmet.isEmpty() ? 0 : MinersSightLogic.getLevel(sp, helmet);
            ArcaneForge.LOGGER.info("[MINERS-SETTINGS] recibido enabled={} filter={} helmet={} level={}",
                    pkt.enabled(), pkt.filterId(), helmet.getItem(), level);
            if (level <= 0) return;

            MinersSightLogic.setEnabled(helmet, pkt.enabled());
            MinersSightLogic.setFilter(helmet, OreFilter.byId(pkt.filterId()));
        });
    }
}
