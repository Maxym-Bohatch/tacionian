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

/**
 * Клас керування енергомережею.
 * Об'єднує групу кабелів у єдиний логічний об'єкт.
 */
public class TachyonNetwork {
    private final Set<TachyonCableBlockEntity> cables = new HashSet<>();
    private int energy = 0;

    // БАЛАНС: Кабелі тепер мають мінімальну ємність (тільки як буфер)
    private final int CAPACITY_PER_CABLE = 10;

    public void addCable(TachyonCableBlockEntity cable) {
        cables.add(cable);
    }

    public void removeCable(TachyonCableBlockEntity cable) {
        cables.remove(cable);
        // Якщо мережа не пуста, можна було б викликати rebuild,
        // але для початкової версії просто залишаємо енергію в "ефірі" мережі
    }

    /**
     * Злиття двох мереж в одну (коли гравець з'єднує їх кабелем)
     */
    public void merge(TachyonNetwork other) {
        if (this == other || other == null) return;

        this.energy += other.energy;
        for (TachyonCableBlockEntity cable : other.cables) {
            cable.setNetwork(this);
            this.cables.add(cable);
        }
        // Обмежуємо енергію новою ємністю, щоб не було дюпів
        this.energy = Math.min(this.energy, getCapacity());
        other.cables.clear();
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

    /**
     * Основна логіка розподілу енергії
     */
    public void tick(Level level) {
        if (energy <= 0 || cables.isEmpty()) return;

        // Перебираємо всі кабелі мережі для пошуку споживачів навколо них
        for (TachyonCableBlockEntity cable : cables) {
            if (energy <= 0) break;
            BlockPos pos = cable.getBlockPos();

            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));

                // Пропускаємо інші кабелі та джерела (Wireless Interface)
                if (neighbor == null || neighbor instanceof TachyonCableBlockEntity
                        || neighbor instanceof WirelessEnergyInterfaceBlockEntity) continue;

                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                    // Намагаємося віддати всю наявну енергію в мережі (до 500 за раз на один блок)
                    int toPush = Math.min(energy, 500);
                    int accepted = cap.receiveTacionEnergy(toPush, false);
                    energy -= accepted;
                });
            }
        }
    }

    /**
     * Оптимізація: тільки один кабель з усієї мережі запускає логіку тику.
     */
    public boolean tickMaster(TachyonCableBlockEntity cable, Level level) {
        if (!cables.isEmpty() && cables.iterator().next() == cable) {
            this.tick(level);
            return true;
        }
        return false;
    }

    public int getEnergy() { return energy; }
    public int getCapacity() { return cables.size() * CAPACITY_PER_CABLE; }
    public boolean isEmpty() { return cables.isEmpty(); }

    private void rebuild(Level level) {
        // Тут буде алгоритм пошуку шляху (BFS), якщо мережа розірвана посередині
    }
}