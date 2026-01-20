package com.maxim.tacionian.api.energy;

public interface ITachyonStorage {
    int receiveTacionEnergy(int amount, boolean simulate);
    int extractTacionEnergy(int amount, boolean simulate);
    int getEnergy();
    int getMaxCapacity();
    // Допоміжний метод, щоб розуміти, чи є місце
    default boolean canReceive() {
        return getEnergy() < getMaxCapacity();
    }
}