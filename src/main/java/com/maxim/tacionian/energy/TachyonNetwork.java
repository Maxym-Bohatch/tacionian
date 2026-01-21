package com.maxim.tacionian.energy;

import com.maxim.tacionian.blocks.cable.TachyonCableBlockEntity;
import com.maxim.tacionian.register.ModCapabilities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class TachyonNetwork {
    private final Set<TachyonCableBlockEntity> cables = new HashSet<>();
    private int energy = 0;
    private final int CAPACITY_PER_CABLE = 1000; // Трохи збільшив ємність для стабільності

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
        if (cables.isEmpty() || level.isClientSide) return;

        // Ефект звуку
        if (energy > 50 && level.random.nextFloat() < 0.005f) {
            BlockPos pos = cables.iterator().next().getBlockPos();
            level.playSound(null, pos, ModSounds.TACHYON_HUM.get(), SoundSource.BLOCKS, 0.05f, 1.0f);
        }

        // РОЗДАЧА ЕНЕРГІЇ (Пасивна)
        // Кабелі тепер ТІЛЬКИ віддають енергію механізмам, які її просять.
        // Більше ніякого агресивного збору (Step 1), щоб не конфліктувати з баками.
        if (energy > 0) {
            for (TachyonCableBlockEntity cable : cables) {
                if (energy <= 0) break;
                BlockPos pos = cable.getBlockPos();

                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                    // Не передаємо іншим кабелям (вони вже в мережі)
                    if (neighbor == null || neighbor instanceof TachyonCableBlockEntity) continue;

                    neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                        // Механізм забирає енергію, тільки якщо він порожніший за мережу (паритет)
                        double netFill = (double) energy / getCapacity();
                        double capFill = (double) cap.getEnergy() / cap.getMaxCapacity();

                        if (netFill > capFill) {
                            int toPush = Math.min(energy, 100);
                            int accepted = cap.receiveTacionEnergy(toPush, false);
                            energy -= accepted;
                        }
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

    public void tickMaster(TachyonCableBlockEntity cable, Level level) {
        if (!cables.isEmpty() && cables.iterator().next() == cable) {
            this.tick(level);
        }
    }

    public int getEnergy() { return energy; }
    public int getCapacity() { return cables.size() * CAPACITY_PER_CABLE; }
}