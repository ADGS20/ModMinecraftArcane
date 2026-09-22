package com.Andres.arcaneforge.event;

import com.Andres.arcaneforge.ArcaneForge;
import com.Andres.arcaneforge.Config;
import com.Andres.arcaneforge.config.ArcaneServerConfig;
import com.Andres.arcaneforge.moon.ArcaneMoonData;
import com.Andres.arcaneforge.moon.ArcaneMoonLogic;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Comandos de administracion para servidores con reglas propias:
 *   /arcaneforge enchant disable <namespace:path>  — prohibe conseguir ESE encantamiento nuevo
 *   /arcaneforge enchant enable  <namespace:path>  — lo vuelve a permitir
 *   /arcaneforge enchant list                       — cuales estan prohibidos ahora mismo
 *   /arcaneforge moon disable|enable|status          — apaga/enciende la Luna Oscura/Roja
 *
 * Requiere permiso de operador (nivel 2, el mismo que /gamemode). El estado
 * vive en ArcaneServerConfig (SavedData de servidor), asi que sobrevive a
 * reinicios sin tocar ningun archivo a mano.
 */
@EventBusSubscriber(modid = ArcaneForge.MODID)
public class ArcaneCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcaneforge")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(enchantNode())
                .then(moonNode())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> enchantNode() {
        return Commands.literal("enchant")
                .then(Commands.literal("disable")
                        .then(Commands.argument("enchantment", StringArgumentType.string())
                                .suggests(ArcaneCommands::suggestEnchantments)
                                .executes(ctx -> setEnchantDisabled(ctx, true))))
                .then(Commands.literal("enable")
                        .then(Commands.argument("enchantment", StringArgumentType.string())
                                .suggests(ArcaneCommands::suggestEnchantments)
                                .executes(ctx -> setEnchantDisabled(ctx, false))))
                .then(Commands.literal("list")
                        .executes(ArcaneCommands::listDisabledEnchantments));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> moonNode() {
        return Commands.literal("moon")
                .then(Commands.literal("enable").executes(ctx -> setMoonEnabled(ctx, true)))
                .then(Commands.literal("disable").executes(ctx -> setMoonEnabled(ctx, false)))
                .then(Commands.literal("status").executes(ArcaneCommands::moonStatus))
                .then(Commands.literal("wave")
                        .then(Commands.argument("ronda", IntegerArgumentType.integer(1, Config.MOON_WAVE_MAX_ROUND))
                                .executes(ctx -> spawnWave(ctx, Config.MOON_WAVE_DEFAULT_COUNT, null))
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, Config.MOON_WAVE_MAX_COUNT))
                                        .executes(ctx -> spawnWave(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"), null))
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(ctx -> spawnWave(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "cantidad"),
                                                        EntityArgument.getPlayer(ctx, "jugador")))))));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static int setEnchantDisabled(CommandContext<CommandSourceStack> ctx, boolean disabled) {
        CommandSourceStack src = ctx.getSource();
        String raw = StringArgumentType.getString(ctx, "enchantment");
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            src.sendFailure(Component.literal("§c'" + raw + "' no es un identificador valido (usa namespace:path, ej. arcaneforge:cadena_arcana)."));
            return 0;
        }

        ServerLevel overworld = overworld(ctx);
        boolean exists = overworld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id)).isPresent();
        if (!exists) {
            src.sendFailure(Component.literal("§c No existe ningun encantamiento registrado con id '" + id + "'."));
            return 0;
        }

        ArcaneServerConfig cfg = ArcaneServerConfig.get(overworld);
        boolean changed = disabled ? cfg.disableEnchantment(id) : cfg.enableEnchantment(id);
        if (!changed) {
            src.sendSuccess(() -> Component.literal("§7'" + id + "' ya estaba " + (disabled ? "deshabilitado" : "habilitado") + "."), true);
            return 1;
        }

        src.sendSuccess(() -> Component.literal(disabled
                ? "§c✔ '" + id + "' deshabilitado: nadie podra conseguirlo nuevo (Forja Arcana ni gear de la Luna Oscura)."
                : "§a✔ '" + id + "' vuelve a estar permitido."), true);
        return 1;
    }

    private static int listDisabledEnchantments(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        var disabled = ArcaneServerConfig.get(overworld(ctx)).getDisabledEnchantments();
        if (disabled.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Ningun encantamiento esta deshabilitado ahora mismo."), false);
        } else {
            src.sendSuccess(() -> Component.literal("§7Encantamientos deshabilitados: §c" + String.join(", ", disabled)), false);
        }
        return 1;
    }

    private static int setMoonEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel overworld = overworld(ctx);
        ArcaneServerConfig cfg = ArcaneServerConfig.get(overworld);
        cfg.setMoonEventEnabled(enabled);

        if (!enabled) {
            ArcaneMoonData data = ArcaneMoonData.get(overworld);
            if (data.isMoonActive()) {
                data.endMoon();
                ArcaneMoonHandler.syncMoonState(ctx.getSource().getServer(), false);
            }
        }

        src.sendSuccess(() -> Component.literal(enabled
                ? "§a✔ La Luna Oscura/Roja vuelve a estar activa."
                : "§c✔ La Luna Oscura/Roja quedo deshabilitada (si estaba activa, se corto de inmediato)."), true);
        return 1;
    }

    private static int moonStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel overworld = overworld(ctx);
        ArcaneMoonData data = ArcaneMoonData.get(overworld);
        boolean enabled = ArcaneServerConfig.get(overworld).isMoonEventEnabled();

        src.sendSuccess(() -> Component.literal(String.format(
                "§7Luna Oscura/Roja: %s §7| Ronda actual: §e%d §7| Activa esta noche: %s",
                enabled ? "§ahabilitada" : "§cdeshabilitada",
                data.getRound(),
                data.isMoonActive() ? "§asi" : "§7no")), false);
        return 1;
    }

    /**
     * /arcaneforge moon wave <ronda> [cantidad] [jugador] — invoca de golpe
     * una oleada de mobs equipados como si fueran de esa ronda, sin tocar la
     * ronda real guardada ni el temporizador de la Luna. Para cuando un
     * admin quiere subir la dificultad de golpe (estilo evento de servidor),
     * no para el goteo automatico de cada noche.
     */
    private static int spawnWave(CommandContext<CommandSourceStack> ctx, int count, ServerPlayer explicitTarget) {
        CommandSourceStack src = ctx.getSource();
        int round = IntegerArgumentType.getInteger(ctx, "ronda");

        ServerPlayer target = explicitTarget;
        if (target == null) {
            if (!(src.getEntity() instanceof ServerPlayer sp)) {
                src.sendFailure(Component.literal("§cEspecifica un jugador: /arcaneforge moon wave " + round + " " + count + " <jugador>."));
                return 0;
            }
            target = sp;
        }

        ServerLevel level = (ServerLevel) target.level();
        ArcaneMoonLogic.spawnWave(target, level, level.getRandom(), round, true, count, true);

        ServerPlayer finalTarget = target;
        src.sendSuccess(() -> Component.literal("§4✔ Oleada invocada: §e" + count
                + " mobs §7a nivel de ronda §e" + round + "§7 junto a §f" + finalTarget.getName().getString() + "§7."), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestEnchantments(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        overworld(ctx).registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .forEach(holder -> {
                    Identifier id = holder.key().identifier();
                    if (id.getNamespace().equals(ArcaneForge.MODID)) {
                        builder.suggest(id.toString());
                    }
                });
        return builder.buildFuture();
    }
}
