package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, level;
    private static float experience;
    private static boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;
    private static boolean initialized = false;

    public static void update(int en, int lvl, float exp, boolean disc, boolean regen, boolean inter, boolean plate, boolean remote) {
        energy = en;
        level = lvl;
        experience = exp;
        disconnected = disc;
        regenBlocked = regen;
        interfaceStabilized = inter;
        plateStabilized = plate;
        remoteNoDrain = remote;
        initialized = true;
    }

    public static boolean hasData() { return initialized; }
    public static int getEnergy() { return energy; }
    public static int getLevel() { return level; }
    public static float getExperience() { return experience; }

    // Визначає колір для тексту енергії в HUD
    public static int getHUDColor() {
        if (plateStabilized) return 0x55FF55; // Світло-зелений (Стабілізація)
        if (energy > getMaxEnergy()) return 0xFF5555; // Червоний (Перевантаження)
        return 0x55FFFF; // Блакитний (Норма)
    }

    public static float getRatio() {
        return (float) energy / getMaxEnergy();
    }

    public static int getMaxEnergy() {
        return 1000 + (Math.max(1, level) - 1) * 500;
    }

    public static int getRequiredExp() {
        return level < 20 ? 500 + (level * 100) : 2500 + (level * 250);
    }

    public static float getExpRatio() {
        int req = getRequiredExp();
        return req > 0 ? (float) experience / req : 0;
    }

    public static boolean isInterfaceStabilized() { return interfaceStabilized; }
    public static boolean isPlateStabilized() { return plateStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
    public static boolean isDisconnected() { return disconnected; }
}