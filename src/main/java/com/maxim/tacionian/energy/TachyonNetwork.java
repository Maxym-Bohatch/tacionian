package com.maxim.tacionian.energy;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.blocks.cable.TachyonCableBlockEntity;
import com.maxim.tacionian.blocks.wireless.WirelessEnergyInterfaceBlockEntity;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class TachyonNetwork {
    private final Set<TachyonCableBlockEntity> cables = new HashSet<>();
    private int energy = 0;
    private final int CAPACITY_PER_CABLE = 100; // Буфер мережі

    public void addCable(TachyonCableBlockEntity cable) { cables.add(cable); }
    public void removeCable(TachyonCableBlockEntity cable) { cables.remove(cable); }

    public void merge(TachyonNetwork other) {
        if (this == other || other == null) return;
        this.energy += other.energy;
        for (TachyonCableBlockEntity cable : other.cables) {
            cable.setNetwork(this);
            this.cables.add(cable);
        }
        this.energy = Math.min(this.energy, getCapacity());
        other.cables.clear();
    }

    public void tick(Level level) {
        if (cables.isEmpty()) return;

        // КРОК 1: ЗБІР (Тільки з джерел та повних резервуарів)
        for (TachyonCableBlockEntity cable : cables) {
            if (energy >= getCapacity()) break;
            BlockPos pos = cable.getBlockPos();

            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null || neighbor instanceof TachyonCableBlockEntity) continue;

                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                    // Якщо в сусіді енергії більше, ніж у мережі (буфері), витягуємо її
                    // Це дозволяє енергії текти від "повного" до "порожнього"
                    if (cap.getEnergy() > 0) {
                        int toPull = Math.min(getCapacity() - energy, 200);
                        int extracted = cap.extractTacionEnergy(toPull, false);
                        energy += extracted;
                    }
                });
            }
        }

        // КРОК 2: РОЗДАЧА (Тільки тим, у кого енергії менше, ніж у мережі)
        if (energy > 0) {
            for (TachyonCableBlockEntity cable : cables) {
                if (energy <= 0) break;
                BlockPos pos = cable.getBlockPos();

                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                    if (neighbor == null || neighbor instanceof TachyonCableBlockEntity) continue;

                    // Не повертаємо енергію в джерело
                    if (neighbor instanceof WirelessEnergyInterfaceBlockEntity) continue;

                    neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                        // Штовхаємо енергію далі
                        int toPush = Math.min(energy, 200);
                        int accepted = cap.receiveTacionEnergy(toPush, false);
                        energy -= accepted;
                    });
                }
            }
        }
    }

    public int receiveEnergy(int amount, boolean simulate) {
        int space = getCapacity() - energy;
        int toAdd = Math.min(amount, space);
        if (!simulate) energy += toAdd;
        return toAdd;
    }

    public int extractEnergy(int amount, boolean simulate) {
        int toTake = Math.min(energy, amount);
        if (!simulate) energy -= toTake;
        return toTake;
    }

    public boolean tickMaster(TachyonCableBlockEntity cable, Level level) {
        if (!cables.isEmpty() && cables.iterator().next() == cable) {
            this.tick(level);
            return true;
        }
        return false;
    }

    public int getEnergy() { return energy; }
    public int getCapacity() { return cables.size() * CAPACITY_PER_CABLE; }
}