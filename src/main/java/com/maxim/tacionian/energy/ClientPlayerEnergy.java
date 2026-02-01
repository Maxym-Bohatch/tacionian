package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, level, maxEnergy, stabilizedTimer, requiredExp;
    private static float experience;
    private static boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;
    private static boolean remoteBlocked, pushbackEnabled, globalStabilized;
    private static boolean initialized = false;

    public static void update(int en, int lvl, int maxEn, int stabT, float exp, int reqExp, boolean disc, boolean regen,
                              boolean inter, boolean plate, boolean remote, boolean remBlock, boolean pushEn, boolean globalStab) {
        energy = en;
        level = lvl;
        maxEnergy = maxEn;
        stabilizedTimer = stabT;
        experience = exp;
        requiredExp = reqExp;

        disconnected = disc;
        regenBlocked = regen;
        interfaceStabilized = inter;
        plateStabilized = plate;
        remoteNoDrain = remote;
        remoteBlocked = remBlock;
        pushbackEnabled = pushEn;
        globalStabilized = globalStab;

        initialized = true;
    }

    public static float getExpRatio() {
        if (requiredExp <= 0) return 0.0f;
        return Math.min(experience / (float) requiredExp, 1.0f);
    }

    // Решта методів без змін...
    public static int getLevel() { return level; }
    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy; }
    public static float getRatio() { return maxEnergy > 0 ? (float) energy / maxEnergy : 0; }
    public static boolean hasData() { return initialized; }
    public static boolean isPlateStabilized() { return plateStabilized; }
    public static boolean isInterfaceStabilized() { return interfaceStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
}