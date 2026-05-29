package com.Andres.arcaneforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ArcaneEnchantments — Data Component propio del mod.
 *
 * CORRECCIÓN 26.1.2:
 *   ResourceLocation → Identifier
 *   (Mojang renombró la clase en Minecraft 26.x)
 */
public final class ArcaneEnchantments {

    public static final Codec<ArcaneEnchantments> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, 100_000))
                            .fieldOf("levels")
                            .forGetter(ArcaneEnchantments::getLevels)
            ).apply(instance, ArcaneEnchantments::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneEnchantments> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(
                            HashMap::new,
                            Identifier.STREAM_CODEC,
                            ByteBufCodecs.VAR_INT
                    ),
                    ArcaneEnchantments::getLevels,
                    ArcaneEnchantments::new
            );

    private final Map<Identifier, Integer> levels;

    public ArcaneEnchantments(Map<Identifier, Integer> levels) {
        this.levels = Map.copyOf(levels);
    }

    public Map<Identifier, Integer> getLevels() {
        return levels;
    }

    public int getLevel(Identifier enchantmentId) {
        return levels.getOrDefault(enchantmentId, 0);
    }

    public ArcaneEnchantments withLevel(Identifier enchantmentId, int level) {
        Map<Identifier, Integer> newMap = new HashMap<>(this.levels);
        if (level <= 0) {
            newMap.remove(enchantmentId);
        } else {
            newMap.put(enchantmentId, level);
        }
        return new ArcaneEnchantments(newMap);
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public static ArcaneEnchantments empty() {
        return new ArcaneEnchantments(Map.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArcaneEnchantments other)) return false;
        return Objects.equals(levels, other.levels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(levels);
    }

    @Override
    public String toString() {
        return "ArcaneEnchantments" + levels;
    }
}