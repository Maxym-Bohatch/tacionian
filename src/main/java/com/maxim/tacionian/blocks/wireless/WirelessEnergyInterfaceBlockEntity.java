package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity {
    private int mode = 0; // 0:Safe, 1:Balanced, 2:Performance, 3:Unrestricted

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    // Метод для перемикання режимів
    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        if (level != null && !level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName()), true);
        }
        setChanged(); // Зберігаємо стан блоку
    }

    // Статичний метод tick для Forge
    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        AABB area = new AABB(pos).inflate(10);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        int threshold = switch (be.mode) {
            case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0;
        };

        for (Player player : players) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                pEnergy.setRemoteStabilized(true);

                if (pEnergy.getEnergyPercent() > threshold) {
                    int txToTake = 20;
                    // Виклик без level.getGameTime()
                    int extracted = pEnergy.extractEnergyWithExp(txToTake, false, player);

                    if (extracted > 0) {
                        for (Direction dir : Direction.values()) {
                            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                            if (neighbor != null) {
                                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                                        .ifPresent(cap -> cap.receiveEnergy(extracted * 10, false));
                            }
                        }
                    }
                }
            });
        }
    }

    public Component getModeName() {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe");
            case 1 -> Component.translatable("mode.tacionian.balanced");
            case 2 -> Component.translatable("mode.tacionian.performance");
            default -> Component.translatable("mode.tacionian.unrestricted");
        };
    }

    // Збереження режиму при перезавантаженні світу
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("Mode", mode);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.mode = nbt.getInt("Mode");
    }
}