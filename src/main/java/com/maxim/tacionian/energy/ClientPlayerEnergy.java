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

    // Формула для розрахунку відсотків основного бару
    public static float getRatio() {
        return (float) energy / getMaxEnergy();
    }

    // Максимальна енергія (синхронізовано з сервером)
    public static int getMaxEnergy() {
        return 1000 + (Math.max(1, level) - 1) * 500;
    }

    // Повертає скільки треба досвіду для наступного рівня (синхронізовано з сервером)
    public static int getRequiredExp() {
        return level < 20 ? 500 + (level * 100) : 2500 + (level * 250);
    }

    // Розрахунок прогресу смужки досвіду (від 0.0 до 1.0)
    public static float getExpRatio() {
        int req = getRequiredExp();
        return req > 0 ? (float) experience / req : 0;
    }

    // Статуси для іконок та кольорів
    public static boolean isInterfaceStabilized() { return interfaceStabilized; }
    public static boolean isPlateStabilized() { return plateStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
}