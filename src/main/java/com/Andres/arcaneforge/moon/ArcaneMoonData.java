package com.Andres.arcaneforge.moon;

import com.Andres.arcaneforge.ArcaneForge;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Estado global de la Luna Oscura/Roja: una sola instancia por servidor
 * (via MinecraftServer.getDataStorage(), el mismo almacen "de verdad global"
 * que usa vanilla para WeatherData/ScoreboardSaveData — NO el de un nivel
 * concreto, que es por-dimension). Funciona igual con 1 jugador o con 10.
 */
public class ArcaneMoonData extends SavedData {

    public static final Codec<ArcaneMoonData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("round", 0).forGetter(d -> d.round),
            Codec.LONG.optionalFieldOf("last_moon_game_time", -1L).forGetter(d -> d.lastMoonGameTime),
            Codec.BOOL.optionalFieldOf("moon_active", false).forGetter(d -> d.moonActive),
            Codec.LONG.optionalFieldOf("moon_end_game_time", -1L).forGetter(d -> d.moonEndGameTime),
            Codec.LONG.optionalFieldOf("last_accelerator_use_game_time", -1L).forGetter(d -> d.lastAcceleratorUseGameTime)
    ).apply(i, ArcaneMoonData::new));

    public static final SavedDataType<ArcaneMoonData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneForge.MODID, "moon_state"), ArcaneMoonData::new, CODEC);

    private int round;
    private long lastMoonGameTime;
    private boolean moonActive;
    private long moonEndGameTime;
    private long lastAcceleratorUseGameTime;

    public ArcaneMoonData() {
        this(0, -1L, false, -1L, -1L);
    }

    public ArcaneMoonData(int round, long lastMoonGameTime, boolean moonActive, long moonEndGameTime, long lastAcceleratorUseGameTime) {
        this.round = round;
        this.lastMoonGameTime = lastMoonGameTime;
        this.moonActive = moonActive;
        this.moonEndGameTime = moonEndGameTime;
        this.lastAcceleratorUseGameTime = lastAcceleratorUseGameTime;
    }

    public static ArcaneMoonData get(ServerLevel any) {
        return any.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public int getRound() {
        return round;
    }

    public boolean isMoonActive() {
        return moonActive;
    }

    public long getLastMoonGameTime() {
        return lastMoonGameTime;
    }

    public long getMoonEndGameTime() {
        return moonEndGameTime;
    }

    public long getLastAcceleratorUseGameTime() {
        return lastAcceleratorUseGameTime;
    }

    /** Sube la ronda y marca la Luna como activa hasta endGameTime (proximo amanecer). */
    public void startMoon(long gameTime, long endGameTime) {
        this.round++;
        this.moonActive = true;
        this.lastMoonGameTime = gameTime;
        this.moonEndGameTime = endGameTime;
        setDirty();
    }

    public void endMoon() {
        this.moonActive = false;
        setDirty();
    }

    /** Adelanta la proxima Luna restando ticks al ultimo disparo registrado. */
    public void useAccelerator(long gameTime, long daysToSkipTicks) {
        this.lastMoonGameTime -= daysToSkipTicks;
        this.lastAcceleratorUseGameTime = gameTime;
        setDirty();
    }
}
