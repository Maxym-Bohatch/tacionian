package com.maxim.tacionian.energy;

import net.minecraftforge.energy.EnergyStorage;

public class TachyonEnergyStorage extends EnergyStorage {

    public TachyonEnergyStorage(int capacity) {
        super(capacity);
    }

    public void addRF(int amount) {
        receiveEnergy(amount, false);
    }
}
