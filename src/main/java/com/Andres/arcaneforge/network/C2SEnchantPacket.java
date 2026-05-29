package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.block.ArcanePedestalBlock; // Importamos el bloque del pedestal
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

            // 🛡️ SEGURIDAD MULTIJUGADOR (Sugerencia 13): Validar distancia servidor-cliente (máx 8 bloques de distancia)
            BlockPos pos = pkt.forgePos();
            if (serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                ArcaneForge.LOGGER.warn("¡El jugador {} intentó enviar un paquete de encantamiento desde una distancia lejana!", serverPlayer.getName().getString());
                return;
            }

            BlockEntity be = serverPlayer.level().getBlockEntity(pos);
            if (!(be instanceof ArcaneForgeBlockEntity forge)) return;

            // CÁLCULO DEL COSTO DE EXP
            float multiplier   = ArcaneForgeBlockEntity.getEnchantmentMultiplier(pkt.enchantmentId());
            int xpLevelCost    = Math.max(1, (int) (pkt.targetLevel() * 3 * multiplier));

            // Comprobación de EXP en supervivencia ANTES de tocar el fuel/materiales
            if (!serverPlayer.isCreative() && serverPlayer.experienceLevel < xpLevelCost) {
                serverPlayer.connection.send(new S2CResultPacket(false,
                        "✖ Falta EXP mística. Requiere §e" + xpLevelCost
                                + " niveles§r de experiencia para este encantamiento."));
                return;
            }

            // EJECUTAR EL ENCANTAMIENTO
            int finalResultLevel = forge.tryEnchant(pkt.enchantmentId(), pkt.targetLevel(), serverPlayer);
            boolean success = finalResultLevel != -1;

            if (success) {
                // Cobrar EXP al jugador (solo en supervivencia)
                if (!serverPlayer.isCreative()) {
                    serverPlayer.giveExperienceLevels(-xpLevelCost);
                }

                // Sincronización oficial NeoForge del slot del contenedor
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                        serverPlayer.containerMenu.containerId,
                        serverPlayer.containerMenu.incrementStateId(),
                        0,
                        forge.getItem(0)
                ));
            }

            // Mensaje informativo al jugador
            String msg = success
                    ? "⚡ ¡Encantamiento aplicado! Nivel final: §a" + finalResultLevel
                      + "§r | EXP consumida: §e" + xpLevelCost + " niveles"
                    : "✖ Materiales insuficientes en los cofres para completar el encantamiento.";

            // Enviar paquetes de red: resultado + sincronización de la GUI
            serverPlayer.connection.send(new S2CResultPacket(success, msg));

            // Calcular el breakdown actualizado para sincronizar la GUI
            ArcaneForgeBlockEntity.FuelBreakdown bd = forge.computeFuelBreakdown();

            // CORRECCIÓN: Comprobamos el pedestal usando el método estático real del bloque en el nivel del servidor
            boolean hasPedestal = ArcanePedestalBlock.hasActivePedestalNearby(forge.getLevel(), forge.getBlockPos());

            serverPlayer.connection.send(new S2CSyncPacket(
                    pkt.forgePos(),
                    forge.getLinkedChestCount(),
                    forge.getNearbyBookshelfCount(),
                    forge.getTotalMagicFuel(),
                    hasPedestal, // Enviamos el estado real corregido
                    bd.common, bd.uncommon, bd.rare, bd.epic, bd.legendary
            ));
        });
    }
}