/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.energy;

public class ClientPlayerEnergy {
    private static int energy, level, maxEnergy;
    private static float experience;
    private static boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;
    private static boolean initialized = false;

    public static void update(int en, int lvl, int maxEn, float exp, boolean disc, boolean regen, boolean inter, boolean plate, boolean remote) {
        energy = en; level = lvl; maxEnergy = maxEn; experience = exp;
        disconnected = disc; regenBlocked = regen;
        interfaceStabilized = inter; plateStabilized = plate; remoteNoDrain = remote;
        initialized = true;
    }

    public static boolean hasData() { return initialized; }
    public static int getEnergy() { return energy; }
    public static int getMaxEnergy() { return maxEnergy > 0 ? maxEnergy : 1000; }
    public static int getLevel() { return level; }
    public static float getRatio() { return maxEnergy > 0 ? (float) energy / maxEnergy : 0; }
    public static float getExpRatio() {
        int req = level < 20 ? 500 + (level * 100) : 2500 + (level * 250);
        return req > 0 ? (float) experience / req : 0;
    }

    public static boolean isInterfaceStabilized() { return interfaceStabilized; }
    public static boolean isPlateStabilized() { return plateStabilized; }
    public static boolean isRemoteNoDrain() { return remoteNoDrain; }
}