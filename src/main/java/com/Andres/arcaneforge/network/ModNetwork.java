package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registra todos los paquetes de red del mod Arcane Forge.
 *
 * CORRECCIÓN IMPORTANTE para NeoForge 26.1.2:
 * StreamCodec NO es una interfaz funcional — tiene dos métodos abstractos
 * (encode + decode), así que NO se puede usar un method reference.
 * Se crean instancias anónimas de StreamCodec para cada paquete.
 */
public class ModNetwork {

    // ── StreamCodec para C2SEnchantPacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SEnchantPacket> ENCHANT_CODEC =
            new StreamCodec<>() {
                @Override
                public C2SEnchantPacket decode(RegistryFriendlyByteBuf buf) {
                    return C2SEnchantPacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, C2SEnchantPacket pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CSyncPacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncPacket> SYNC_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CSyncPacket decode(RegistryFriendlyByteBuf buf) {
                    return S2CSyncPacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CSyncPacket pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CResultPacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CResultPacket> RESULT_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CResultPacket decode(RegistryFriendlyByteBuf buf) {
                    return S2CResultPacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CResultPacket pkt) {
                    pkt.write(buf);
                }
            };

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloadHandlers);
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ArcaneForge.MODID)
                .versioned("2.0.0");

        // Cliente → Servidor: solicitud de encantamiento
        registrar.playToServer(
                C2SEnchantPacket.TYPE,
                ENCHANT_CODEC,
                C2SEnchantPacket::handle
        );

        // Servidor → Cliente: sync de datos
        registrar.playToClient(
                S2CSyncPacket.TYPE,
                SYNC_CODEC,
                S2CSyncPacket::handle
        );

        // Servidor → Cliente: resultado de encantamiento
        registrar.playToClient(
                S2CResultPacket.TYPE,
                RESULT_CODEC,
                S2CResultPacket::handle
        );

        ArcaneForge.LOGGER.info("Arcane Forge FUSION network packets registered (v2.0).");
    }
}
