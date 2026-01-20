package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, maxEnergy, level, experience, threshold;
    private static boolean stabilized, remoteStabilized, remoteNoDrain;

    public static void receiveSync(int energy, int level, int experience, int threshold, boolean stabilized, boolean remoteStabilized, boolean remoteNoDrain) {
        ClientPlayerEnergy.energy = energy;
        ClientPlayerEnergy.level = level;
        ClientPlayerEnergy.experience = experience;
        ClientPlayerEnergy.threshold = threshold;
        ClientPlayerEnergy.stabilized = stabilized;
        ClientPlayerEnergy.remoteStabilized = remoteStabilized;
        ClientPlayerEnergy.remoteNoDrain = remoteNoDrain;
        ClientPlayerEnergy.maxEnergy = 1000 + (level - 1) * 500;
    }

    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy; }
    public static int getLevel() { return level; }
    public static int getExperience() { return experience; }
    public static int getRequiredExp() { return level * 500 + (level * 100); }
    public static float getRatio() { return maxEnergy > 0 ? (float) energy / maxEnergy : 0; }
    public static boolean isStabilized() { return stabilized; }
    public static boolean isRemoteStabilized() { return remoteStabilized; }
    public static boolean hasData() { return maxEnergy > 0; }
    public static boolean isCriticalLow() { return getRatio() < 0.15f; }
    public static boolean isOverloaded() { return getRatio() > 0.97f; }
}