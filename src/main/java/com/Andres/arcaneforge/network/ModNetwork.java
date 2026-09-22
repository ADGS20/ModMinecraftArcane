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

    // ── StreamCodec para C2SBagPagePacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SBagPagePacket> BAG_PAGE_CODEC =
            new StreamCodec<>() {
                @Override
                public C2SBagPagePacket decode(RegistryFriendlyByteBuf buf) {
                    return C2SBagPagePacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, C2SBagPagePacket pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CBagPageSync ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBagPageSync> BAG_PAGE_SYNC_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CBagPageSync decode(RegistryFriendlyByteBuf buf) {
                    return S2CBagPageSync.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CBagPageSync pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para C2SToggleMagnetPacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SToggleMagnetPacket> TOGGLE_MAGNET_CODEC =
            new StreamCodec<>() {
                @Override
                public C2SToggleMagnetPacket decode(RegistryFriendlyByteBuf buf) {
                    return C2SToggleMagnetPacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, C2SToggleMagnetPacket pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CBagMagnetSync ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBagMagnetSync> BAG_MAGNET_SYNC_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CBagMagnetSync decode(RegistryFriendlyByteBuf buf) {
                    return S2CBagMagnetSync.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CBagMagnetSync pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CInfiniteChestSync ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CInfiniteChestSync> INFINITE_CHEST_SYNC_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CInfiniteChestSync decode(RegistryFriendlyByteBuf buf) {
                    return S2CInfiniteChestSync.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CInfiniteChestSync pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para C2SMinersSightSettingsPacket ──
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMinersSightSettingsPacket> MINERS_SIGHT_CODEC =
            new StreamCodec<>() {
                @Override
                public C2SMinersSightSettingsPacket decode(RegistryFriendlyByteBuf buf) {
                    return C2SMinersSightSettingsPacket.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, C2SMinersSightSettingsPacket pkt) {
                    pkt.write(buf);
                }
            };

    // ── StreamCodec para S2CMoonStateSync ──
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMoonStateSync> MOON_STATE_SYNC_CODEC =
            new StreamCodec<>() {
                @Override
                public S2CMoonStateSync decode(RegistryFriendlyByteBuf buf) {
                    return S2CMoonStateSync.read(buf);
                }
                @Override
                public void encode(RegistryFriendlyByteBuf buf, S2CMoonStateSync pkt) {
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

        // Cliente → Servidor: cambiar pagina del Saco Dimensional
        registrar.playToServer(
                C2SBagPagePacket.TYPE,
                BAG_PAGE_CODEC,
                C2SBagPagePacket::handle
        );

        // Servidor → Cliente: sincronizar numero de pagina del saco
        registrar.playToClient(
                S2CBagPageSync.TYPE,
                BAG_PAGE_SYNC_CODEC,
                S2CBagPageSync::handle
        );

        // Cliente → Servidor: activar/desactivar el iman de la bolsa
        registrar.playToServer(
                C2SToggleMagnetPacket.TYPE,
                TOGGLE_MAGNET_CODEC,
                C2SToggleMagnetPacket::handle
        );

        // Servidor → Cliente: sincronizar estado del iman de la bolsa
        registrar.playToClient(
                S2CBagMagnetSync.TYPE,
                BAG_MAGNET_SYNC_CODEC,
                S2CBagMagnetSync::handle
        );

        // Cliente → Servidor: nuevo estado de la Vision Minera
        registrar.playToServer(
                C2SMinersSightSettingsPacket.TYPE,
                MINERS_SIGHT_CODEC,
                C2SMinersSightSettingsPacket::handle
        );

        // Servidor → Cliente: avisa que el ChestMenu abierto es un Cofre Infinito
        registrar.playToClient(
                S2CInfiniteChestSync.TYPE,
                INFINITE_CHEST_SYNC_CODEC,
                S2CInfiniteChestSync::handle
        );

        // Servidor → Cliente: estado de la Luna Oscura/Roja (para teñir el cielo)
        registrar.playToClient(
                S2CMoonStateSync.TYPE,
                MOON_STATE_SYNC_CODEC,
                S2CMoonStateSync::handle
        );

        ArcaneForge.LOGGER.info("Arcane Forge FUSION network packets registered (v2.0).");
    }
}
