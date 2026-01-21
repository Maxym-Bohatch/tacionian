package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, maxEnergy, level, experience, requiredExp, customColor;
    private static boolean stabilized, remoteStabilized, remoteNoDrain, jammed;

    public static void receiveSync(int energy, int level, int experience, int maxEnergy, int requiredExp,
                                   boolean stabilized, boolean remoteStabilized, boolean remoteNoDrain, boolean jammed, int customColor) {
        ClientPlayerEnergy.energy = energy;
        ClientPlayerEnergy.level = level;
        ClientPlayerEnergy.experience = experience;
        ClientPlayerEnergy.maxEnergy = maxEnergy;
        ClientPlayerEnergy.requiredExp = requiredExp;
        ClientPlayerEnergy.stabilized = stabilized;
        ClientPlayerEnergy.remoteStabilized = remoteStabilized;
        ClientPlayerEnergy.remoteNoDrain = remoteNoDrain;
        ClientPlayerEnergy.jammed = jammed;
        ClientPlayerEnergy.customColor = customColor;
    }

    public static boolean isOverloaded() { float threshold = (level <= 5) ? 0.95f : 0.8f;
        return getRatio() >= threshold; }
    public static boolean isCriticalOverload() { return getRatio() >= 0.95f; }
    public static boolean isCriticalLow() { return getRatio() < 0.05f; }

    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy; }
    public static int getLevel() { return level; }
    public static float getRatio() { return maxEnergy > 0 ? (float) energy / maxEnergy : 0; }
    public static int getExperience() { return experience; }
    public static int getRequiredExp() { return requiredExp; }
    public static int getCustomColor() { return customColor; }
    public static boolean isStabilized() { return stabilized; }
    public static boolean isRemoteStabilized() { return remoteStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
    public static boolean isJammed() { return jammed; }
    public static boolean hasData() { return maxEnergy > 0; }
}