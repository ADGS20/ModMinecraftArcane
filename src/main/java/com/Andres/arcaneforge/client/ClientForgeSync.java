package com.Andres.arcaneforge.client;

import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.network.S2CSyncPacket;

/**
 * Codigo que SOLO se ejecuta en el cliente.
 *
 * Esta clase esta separada a proposito: toca net.minecraft.client.Minecraft,
 * que NO existe en un servidor dedicado. Al estar aislada aqui, el servidor
 * nunca necesita cargar esta clase, y por eso no crashea al arrancar.
 *
 * El paquete S2CSyncPacket solo llama a este metodo cuando comprueba que esta
 * en el lado cliente (ver S2CSyncPacket.handle).
 */
public final class ClientForgeSync {

    private ClientForgeSync() {}

    /** Aplica en el cliente los datos recibidos del servidor. */
    public static void apply(S2CSyncPacket pkt) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var be = mc.level.getBlockEntity(pkt.forgePos());
        if (!(be instanceof ArcaneForgeBlockEntity forge)) return;

        // Datos basicos
        forge.setClientSyncData(
                pkt.linkedChests(),
                pkt.bookshelves(),
                pkt.magicFuel(),
                pkt.hasActivePedestal()
        );

        // Desglose de fuel por tier
        forge.setClientFuelBreakdown(
                pkt.fuelCommon(),
                pkt.fuelUncommon(),
                pkt.fuelRare(),
                pkt.fuelEpic(),
                pkt.fuelLegendary()
        );
    }
}
