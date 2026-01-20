package com.maxim.tacionian.api.energy;

public interface ITachyonStorage {
    int receiveTacion(int amount, boolean simulate);
    int extractTacion(int amount, boolean simulate);
    int getTacionStored();
    int getMaxTacionCapacity();

    // Допоміжний метод, щоб розуміти, чи є місце
    default boolean canReceive() {
        return getTacionStored() < getMaxTacionCapacity();
    }
}