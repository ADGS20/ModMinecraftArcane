package com.Andres.arcaneforge.client;

/**
 * Bandera SOLO cliente: si la Luna Oscura/Roja esta activa ahora mismo,
 * segun el ultimo S2CMoonStateSync recibido del servidor. Usada por
 * event.ArcaneMoonClientHandler para teñir el cielo/niebla de rojo.
 */
public final class ClientMoonState {

    private ClientMoonState() {}

    private static volatile boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
