package com.maxim.tacionian.energy;

import net.minecraftforge.energy.EnergyStorage;

public class TachyonEnergyStorage extends EnergyStorage {
    // Конструктор для кабелів (з лімітами)
    public TachyonEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    // Конструктор для блоків (все в одне)
    public TachyonEnergyStorage(int capacity) {
        super(capacity, capacity, capacity);
    }
    public int getMaxExtract() {
        return this.maxExtract;
    }

    public int getMaxReceive() {
        return this.maxReceive;
    }
    public void setEnergy(int amount) {
        this.energy = amount;
    }
}