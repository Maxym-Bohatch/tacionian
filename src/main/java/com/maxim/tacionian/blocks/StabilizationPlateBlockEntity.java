package com.maxim.tacionian.blocks;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlockEntity extends BlockEntity implements ITachyonStorage {
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 10000;
    private int currentMode = 0;

    public StabilizationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZER_PLATE_BE.get(), pos, state);
    }

    public void cycleMode() {
        this.currentMode = (this.currentMode + 1) % 4;
        setChanged();
    }

    public int getCurrentMode() { return currentMode; }

    public static void tick(Level level, BlockPos pos, BlockState state, StabilizationPlateBlockEntity be) {
        if (level.isClientSide) return;

        AABB area = new AABB(pos.above());
        level.getEntitiesOfClass(ServerPlayer.class, area).forEach(player -> {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                pEnergy.setPlateStabilized(true);
                pEnergy.setRegenBlocked(be.currentMode != 3);

                int thresholdPercent = switch (be.currentMode) {
                    case 0 -> 75;
                    case 1 -> 40;
                    case 2 -> 15;
                    case 3 -> 205; // В Unrestricted дозволяємо до 205%
                    default -> 100; // Стандартний поріг
                };

                int effectiveThreshold = player.isCrouching() ? 0 : (pEnergy.getMaxEnergy() * thresholdPercent) / 100;

                int actuallyExtracted = 0; // Змінна для відстеження кількості викачаної енергії
                if (pEnergy.getEnergy() > effectiveThreshold) {
                    int excess = pEnergy.getEnergy() - effectiveThreshold;
                    int drainRate = (be.currentMode == 3 && player.isCrouching()) ? 80 : 40;
                    int toExtract = drainRate + (excess / 10);

                    actuallyExtracted = pEnergy.extractEnergyPure(toExtract, false); // Зберігаємо результат
                    int accepted = be.receiveTacionEnergy(actuallyExtracted, false);

                    if (level.getGameTime() % 10 == 0) {
                        float pitch = player.isCrouching() ? 0.7f : 1.2f;
                        level.playSound(null, pos, ModSounds.TACHYON_HUM.get(), SoundSource.BLOCKS, 0.25f, pitch);

                        if (actuallyExtracted > 0) {
                            level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.2f, 1.4f);
                        }
                    }

                    if (accepted < actuallyExtracted) {
                        MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, actuallyExtracted - accepted));
                    }
                    pEnergy.sync(player);
                    be.setChanged();
                }

                // --- НОВИЙ КОД ДЛЯ ВІЗУАЛЬНОГО ЕФЕКТУ (ПРОМІНЬ) ---
                if (actuallyExtracted > 0 || (pEnergy.getEnergy() < pEnergy.getMaxEnergy() * thresholdPercent / 100 && be.currentMode == 3 && !player.isCrouching())) {
                    // Умовно вважаємо, що активна взаємодія, якщо або енергія викачується,
                    // або гравець на плиті в режимі 3 (Unrestricted) і заряджається

                    Vec3 playerFeetPos = player.position().add(0, -0.05, 0); // Трохи нижче ніг гравця
                    Vec3 plateCenterPos = Vec3.atCenterOf(pos).add(0, 0.1, 0); // Центр плити, трохи вище

                    double distance = plateCenterPos.distanceTo(playerFeetPos);
                    // Кількість частинок залежить від відстані та "інтенсивності"
                    int particlesPerTick = Math.max(1, (int)(distance * 1.5));

                    // Генеруємо частинки вздовж лінії між плитою та гравцем
                    for (int i = 0; i < particlesPerTick; i++) {
                        double progress = (double)i / particlesPerTick;
                        double x = plateCenterPos.x + (playerFeetPos.x - plateCenterPos.x) * progress;
                        double y = plateCenterPos.y + (playerFeetPos.y - plateCenterPos.y) * progress;
                        double z = plateCenterPos.z + (playerFeetPos.z - plateCenterPos.z) * progress;

                        // Додаємо випадковість для "об'єму" променя
                        double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.2;
                        double offsetY = (level.getRandom().nextDouble() - 0.5) * 0.2;
                        double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.2;

                        ((ServerLevel)level).sendParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                x + offsetX, y + offsetY, z + offsetZ,
                                1, 0, 0, 0, 0
                        );
                    }
                }
                // --- КІНЕЦЬ НОВОГО КОДУ ДЛЯ ВІЗУАЛЬНОГО ЕФЕКТУ ---

            });
        });

        // Передача енергії з буфера плити в сусідні блоки (кабелі/сховища)
        if (be.storedEnergy > 0) {
            for (Direction dir : Direction.values()) {
                if (be.storedEnergy <= 0) break;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor != null && !(neighbor instanceof StabilizationPlateBlockEntity)) {
                    neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                        int pushed = cap.receiveTacionEnergy(Math.min(be.storedEnergy, 500), false);
                        if (pushed > 0) {
                            be.storedEnergy -= pushed;
                            be.setChanged();
                        }
                    });
                }
            }
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("TachyonEnergy");
        this.currentMode = nbt.getInt("StabilizationMode");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("TachyonEnergy", storedEnergy);
        nbt.putInt("StabilizationMode", currentMode);
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int canReceive = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate && canReceive > 0) { storedEnergy += canReceive; setChanged(); }
        return canReceive;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int canExtract = Math.min(amount, storedEnergy);
        if (!simulate && canExtract > 0) { storedEnergy -= canExtract; setChanged(); }
        return canExtract;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return LazyOptional.of(() -> this).cast();
        return super.getCapability(cap, side);
    }
}