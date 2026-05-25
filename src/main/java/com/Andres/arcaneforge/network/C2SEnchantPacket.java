package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente → Servidor: El jugador solicita un encantamiento en la forja.
 *
 * CORRECCIÓN: Usa writeUtf/readUtf + Identifier.parse() en lugar de
 * writeResourceLocation/readResourceLocation (eliminados en 26.1).
 */
public record C2SEnchantPacket(BlockPos forgePos, Identifier enchantmentId, int targetLevel)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<C2SEnchantPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "enchant"));

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(forgePos);
        buf.writeUtf(enchantmentId.toString());
        buf.writeVarInt(targetLevel);
    }

    public static C2SEnchantPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Identifier id = Identifier.parse(buf.readUtf(256));
        int level = buf.readVarInt();
        return new C2SEnchantPacket(pos, id, level);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SEnchantPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;

            BlockEntity be = serverPlayer.level().getBlockEntity(pkt.forgePos());
            if (!(be instanceof ArcaneForgeBlockEntity forge)) {
                ArcaneForge.LOGGER.warn("C2SEnchantPacket: no ArcaneForgeBlockEntity en {}", pkt.forgePos());
                return;
            }

            boolean success = forge.tryEnchant(pkt.enchantmentId(), pkt.targetLevel(), serverPlayer);

            String msg;
            if (success) {
                msg = (pkt.targetLevel() > 5)
                        ? "⚡ Enchantment level " + pkt.targetLevel() + " applied! Arcane Forge breaks vanilla rules!"
                        : "✔ Enchantment applied successfully!";
            } else {
                msg = "✖ Not enough magic fuel or invalid enchantment.";
            }

            S2CResultPacket result = new S2CResultPacket(success, msg);
            S2CSyncPacket sync = new S2CSyncPacket(
                    pkt.forgePos(),
                    forge.getLinkedChestCount(),
                    forge.getNearbyBookshelfCount(),
                    forge.getTotalMagicFuel(),
                    forge.hasActivePedestal()
            );

            serverPlayer.connection.send(result);
            serverPlayer.connection.send(sync);
        });
    }
}
