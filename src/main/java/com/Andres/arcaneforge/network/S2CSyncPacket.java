package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete servidor → cliente que sincroniza el estado de la Arcane Forge:
 *   - Número de cofres vinculados y librerías
 *   - Fuel total y su desglose por tier de material
 *   - Si hay un Pedestal Arcano activo
 *
 * El desglose permite a la GUI mostrar cuánto poder aporta cada categoría
 * sin necesidad de que el cliente tenga acceso a los cofres directamente.
 */
public record S2CSyncPacket(
        BlockPos forgePos,
        int linkedChests,
        int bookshelves,
        int magicFuel,
        boolean hasActivePedestal,
        // ── Desglose de fuel por tier ──────────────────────────────────────
        int fuelCommon,     // Tier 1 (carbón, piedra, hueso…)         valor < 10
        int fuelUncommon,   // Tier 2 (hierro, lapislázuli, redstone…) valor 10–49
        int fuelRare,       // Tier 3 (diamante, esmeralda, eco…)      valor 50–199
        int fuelEpic,       // Tier 4 (netherita, elitras, tótem…)     valor 200–749
        int fuelLegendary   // Tier 5 (manzana dorada, tridente…)      valor >= 750
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "sync"));

    // ── Serialización ────────────────────────────────────────────────────────

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(forgePos);
        buf.writeVarInt(linkedChests);
        buf.writeVarInt(bookshelves);
        buf.writeVarInt(magicFuel);
        buf.writeBoolean(hasActivePedestal);
        buf.writeVarInt(fuelCommon);
        buf.writeVarInt(fuelUncommon);
        buf.writeVarInt(fuelRare);
        buf.writeVarInt(fuelEpic);
        buf.writeVarInt(fuelLegendary);
    }

    public static S2CSyncPacket read(FriendlyByteBuf buf) {
        BlockPos pos          = buf.readBlockPos();
        int chests            = buf.readVarInt();
        int books             = buf.readVarInt();
        int fuel              = buf.readVarInt();
        boolean pedestal      = buf.readBoolean();
        int common            = buf.readVarInt();
        int uncommon          = buf.readVarInt();
        int rare              = buf.readVarInt();
        int epic              = buf.readVarInt();
        int legendary         = buf.readVarInt();
        return new S2CSyncPacket(pos, chests, books, fuel, pedestal,
                common, uncommon, rare, epic, legendary);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── Manejo en el cliente ─────────────────────────────────────────────────

    public static void handle(S2CSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            var be = mc.level.getBlockEntity(pkt.forgePos());
            if (!(be instanceof ArcaneForgeBlockEntity forge)) return;

            // Sincronizar datos básicos
            forge.setClientSyncData(
                    pkt.linkedChests(),
                    pkt.bookshelves(),
                    pkt.magicFuel(),
                    pkt.hasActivePedestal()
            );

            // Sincronizar breakdown por tier
            forge.setClientFuelBreakdown(
                    pkt.fuelCommon(),
                    pkt.fuelUncommon(),
                    pkt.fuelRare(),
                    pkt.fuelEpic(),
                    pkt.fuelLegendary()
            );
        });
    }
}