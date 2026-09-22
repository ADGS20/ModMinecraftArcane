package com.Andres.arcaneforge.config;

import com.Andres.arcaneforge.ArcaneForge;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Config de servidor AJUSTABLE EN VIVO por comando (/arcaneforge), pensada
 * para servidores con reglas estrictas que quieren prohibir encantamientos
 * concretos o apagar sistemas enteros (la Luna Oscura/Roja) sin recompilar
 * el mod ni tocar Config.java (esa clase sigue siendo solo constantes fijas
 * de balance). Vive en el mismo almacen "de verdad global" que ArcaneMoonData
 * (MinecraftServer.getDataStorage()), asi que sobrevive a un reinicio del
 * servidor igual que cualquier otro SavedData de vainilla.
 *
 * IMPORTANTE sobre el alcance real: deshabilitar un encantamiento aqui
 * impide conseguirlo NUEVO (Forja Arcana, gear de la Luna Oscura). NO quita
 * el que un jugador ya tenga puesto en un item existente — eso requeriria
 * escanear inventarios/cofres/chunks cargados y no es lo que hace este
 * sistema.
 */
public class ArcaneServerConfig extends SavedData {

    public static final Codec<ArcaneServerConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("disabled_enchantments", List.of())
                    .forGetter(d -> List.copyOf(d.disabledEnchantments)),
            Codec.BOOL.optionalFieldOf("moon_event_enabled", true).forGetter(d -> d.moonEventEnabled)
    ).apply(i, ArcaneServerConfig::new));

    public static final SavedDataType<ArcaneServerConfig> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "server_config"), ArcaneServerConfig::new, CODEC);

    private final Set<String> disabledEnchantments;
    private boolean moonEventEnabled;

    public ArcaneServerConfig() {
        this(List.of(), true);
    }

    public ArcaneServerConfig(List<String> disabledEnchantments, boolean moonEventEnabled) {
        this.disabledEnchantments = new HashSet<>(disabledEnchantments);
        this.moonEventEnabled = moonEventEnabled;
    }

    public static ArcaneServerConfig get(ServerLevel any) {
        return any.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isEnchantmentDisabled(Identifier id) {
        return disabledEnchantments.contains(id.toString());
    }

    /** @return false si ya estaba deshabilitado (nada que hacer). */
    public boolean disableEnchantment(Identifier id) {
        boolean changed = disabledEnchantments.add(id.toString());
        if (changed) setDirty();
        return changed;
    }

    /** @return false si ya estaba habilitado (nada que hacer). */
    public boolean enableEnchantment(Identifier id) {
        boolean changed = disabledEnchantments.remove(id.toString());
        if (changed) setDirty();
        return changed;
    }

    public Set<String> getDisabledEnchantments() {
        return disabledEnchantments;
    }

    public boolean isMoonEventEnabled() {
        return moonEventEnabled;
    }

    public void setMoonEventEnabled(boolean enabled) {
        if (this.moonEventEnabled == enabled) return;
        this.moonEventEnabled = enabled;
        setDirty();
    }
}
