/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, level, maxEnergy, stabilizedTimer;
    private static float experience;
    private static boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;
    private static boolean remoteBlocked, pushbackEnabled, globalStabilized; // НОВІ ПОЛЯ
    private static boolean initialized = false;

    // Оновлено: тепер приймає 13 параметрів для повної синхронізації з EnergySyncPacket
    public static void update(int en, int lvl, int maxEn, int stabT, float exp, boolean disc, boolean regen,
                              boolean inter, boolean plate, boolean remote, boolean remBlock, boolean pushEn, boolean globalStab) {
        energy = en;
        level = lvl;
        maxEnergy = maxEn;
        stabilizedTimer = stabT;
        experience = exp;
        disconnected = disc;
        regenBlocked = regen;
        interfaceStabilized = inter;
        plateStabilized = plate;
        remoteNoDrain = remote;

        // Нові стани
        remoteBlocked = remBlock;
        pushbackEnabled = pushEn;
        globalStabilized = globalStab;

        initialized = true;
    }

    // Геттери для нових станів (корисно для HUD)
    public static boolean isRemoteBlocked() { return remoteBlocked; }
    public static boolean isPushbackEnabled() { return pushbackEnabled; }
    public static boolean isGloballyProtected() { return globalStabilized; }

    public static boolean isProtected() {
        return (level <= 5) || stabilizedTimer > 0 || interfaceStabilized || plateStabilized || remoteNoDrain;
    }

    public static boolean hasData() { return initialized; }
    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy > 0 ? maxEnergy : 1000; }
    public static int getLevel() { return level; }
    public static float getRatio() { return maxEnergy > 0 ? (float) energy / maxEnergy : 0; }

    public static float getExpRatio() {
        int req = 5000 + (level * 1500);
        return req > 0 ? (float) experience / req : 0;
    }

    public static boolean isDisconnected() { return disconnected; }
    public static boolean isInterfaceStabilized() { return interfaceStabilized; }
    public static boolean isPlateStabilized() { return plateStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
    public static int getStabilizedTimer() { return stabilizedTimer; }
}