package com.Andres.arcaneforge.network;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.block.ArcaneDiscountBlock;
import com.Andres.arcaneforge.block.ArcaneForgeBlockEntity;
import com.Andres.arcaneforge.block.ArcanePedestalBlock; // Importamos el bloque del pedestal
import com.Andres.arcaneforge.config.ArcaneServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

            // targetLevel negativo = el jugador pidió BAJAR el nivel (boton "➖
            // Nivel" de la GUI), no subirlo. Es un camino totalmente aparte: no
            // gasta magic fuel (ya se pago al subirlo la primera vez) y en vez
            // de cobrar EXP, se la reembolsa al jugador.
            if (pkt.targetLevel() < 0) {
                handleLower(pkt, forge, serverPlayer, pos);
                return;
            }

            // Servidores con reglas propias pueden prohibir encantamientos
            // concretos con /arcaneforge enchant disable — solo bloquea
            // CONSEGUIRLO nuevo, no afecta a quien ya lo tenga puesto.
            if (ArcaneServerConfig.get((ServerLevel) serverPlayer.level()).isEnchantmentDisabled(pkt.enchantmentId())) {
                serverPlayer.connection.send(new S2CResultPacket(false,
                        "✖ Este encantamiento esta deshabilitado en este servidor."));
                return;
            }

            // TOPE REAL: si este encantamiento ya llegó al nivel donde su propio
            // efecto se aplana (ver ArcaneForgeBlockEntity.getRealMaxLevel), no
            // dejamos ni intentarlo — así el jugador no gasta magic fuel/EXP para
            // absolutamente nada y no se siente estafado.
            int currentLevel = forge.getCurrentEnchantLevel(pkt.enchantmentId());
            int realMax = ArcaneForgeBlockEntity.getRealMaxLevel(pkt.enchantmentId());
            if (currentLevel >= realMax) {
                serverPlayer.connection.send(new S2CResultPacket(false,
                        "✦ Este encantamiento ya llegó a su nivel máximo real (§e" + realMax
                                + "§r). Subir más no le hace absolutamente nada."));
                return;
            }

            // CÁLCULO DEL COSTO DE EXP
            float multiplier   = ArcaneForgeBlockEntity.getEnchantmentMultiplier(pkt.enchantmentId());
            int xpLevelCost    = Math.max(1, (int) (pkt.targetLevel() * 3 * multiplier));

            // Rama XP de los bloques de descuento del aldeano: abarata este coste en EXP.
            float xpDiscount = ArcaneDiscountBlock.getBestDiscount(serverPlayer.level(), pos, ArcaneDiscountBlock.Branch.XP);
            if (xpDiscount > 0f) xpLevelCost = Math.max(1, Math.round(xpLevelCost * (1.0f - xpDiscount)));

            // Comprobación de EXP en supervivencia ANTES de tocar el fuel/materiales
            if (!serverPlayer.isCreative() && serverPlayer.experienceLevel < xpLevelCost) {
                serverPlayer.connection.send(new S2CResultPacket(false,
                        "✖ Falta EXP mística. Requiere §e" + xpLevelCost
                                + " niveles§r de experiencia para este encantamiento."));
                return;
            }

            // EJECUTAR EL ENCANTAMIENTO
            int finalResultLevel = forge.tryEnchant(pkt.enchantmentId(), pkt.targetLevel(), serverPlayer);

            // -2 = el item ya tiene un encantamiento incompatible con este (Toque
            // de Seda+Fortuna, Filo+Smite/Perdicion, dos Protecciones, etc.) y no
            // hay Pedestal ni Bloque de Poder Arcano cerca para fusionarlos.
            if (finalResultLevel == -2) {
                serverPlayer.connection.send(new S2CResultPacket(false,
                        "✖ Encantamientos incompatibles entre si. Necesitas un §dPedestal Arcano§r o un §dBloque de Poder Arcano§r cerca (radio 5) para fusionarlos."));
                return;
            }

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

            // Descuentos activos (bloques del aldeano cerca de la Forja), solo para mostrarlos en el mensaje.
            float materialDiscount = ArcaneDiscountBlock.getBestDiscount(serverPlayer.level(), pos, ArcaneDiscountBlock.Branch.MATERIAL);
            StringBuilder discountNote = new StringBuilder();
            if (materialDiscount > 0f || xpDiscount > 0f) {
                discountNote.append(" §7(");
                if (materialDiscount > 0f) discountNote.append("§a-").append(Math.round(materialDiscount * 100)).append("%mat§7");
                if (materialDiscount > 0f && xpDiscount > 0f) discountNote.append(" ");
                if (xpDiscount > 0f) discountNote.append("§a-").append(Math.round(xpDiscount * 100)).append("%exp§7");
                discountNote.append(")§r");
            }

            // Mensaje informativo al jugador
            String msg = success
                    ? "⚡ ¡Encantamiento aplicado! Nivel final: §a" + finalResultLevel
                      + "§r | EXP consumida: §e" + xpLevelCost + " niveles" + discountNote
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

    /** Camino de "bajar nivel": sin costo de fuel, reembolsa EXP al jugador. */
    private static void handleLower(C2SEnchantPacket pkt, ArcaneForgeBlockEntity forge, ServerPlayer serverPlayer, BlockPos pos) {
        int levelsToRemove = -pkt.targetLevel();
        int currentLevel = forge.getCurrentEnchantLevel(pkt.enchantmentId());

        if (currentLevel <= 0 || levelsToRemove <= 0) {
            serverPlayer.connection.send(new S2CResultPacket(false,
                    "✖ Este encantamiento no tiene nivel que quitar."));
            return;
        }
        levelsToRemove = Math.min(levelsToRemove, currentLevel);

        // Mismo calculo que el costo de subir (nivel * 3 * rareza), pero
        // aqui es un reembolso: bajar lo mismo que costaria subir de nuevo.
        float multiplier = ArcaneForgeBlockEntity.getEnchantmentMultiplier(pkt.enchantmentId());
        int xpRefund = Math.max(1, (int) (levelsToRemove * 3 * multiplier));

        int finalResultLevel = forge.tryLowerEnchant(pkt.enchantmentId(), levelsToRemove, serverPlayer);
        boolean success = finalResultLevel != -1;

        if (success) {
            if (!serverPlayer.isCreative()) {
                serverPlayer.giveExperienceLevels(xpRefund);
            }
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    serverPlayer.containerMenu.containerId,
                    serverPlayer.containerMenu.incrementStateId(),
                    0,
                    forge.getItem(0)
            ));
        }

        String msg = success
                ? "♦ Nivel reducido a §e" + finalResultLevel + "§r | EXP reembolsada: §a+" + xpRefund + " niveles"
                : "✖ No se pudo bajar el nivel de este encantamiento.";
        serverPlayer.connection.send(new S2CResultPacket(success, msg));

        ArcaneForgeBlockEntity.FuelBreakdown bd = forge.computeFuelBreakdown();
        boolean hasPedestal = ArcanePedestalBlock.hasActivePedestalNearby(forge.getLevel(), forge.getBlockPos());

        serverPlayer.connection.send(new S2CSyncPacket(
                pos,
                forge.getLinkedChestCount(),
                forge.getNearbyBookshelfCount(),
                forge.getTotalMagicFuel(),
                hasPedestal,
                bd.common, bd.uncommon, bd.rare, bd.epic, bd.legendary
        ));
    }
}