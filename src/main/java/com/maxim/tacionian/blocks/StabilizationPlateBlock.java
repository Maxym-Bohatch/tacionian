package com.maxim.tacionian.blocks;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlock extends BaseEntityBlock {
    public StabilizationPlateBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StabilizationPlateBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STABILIZER_PLATE_BE.get(), StabilizationPlateBlockEntity::tick);
    }

    // Перемикання режимів плити на ПКМ
    // У методі use(...) виправлений рядок:
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StabilizationPlateBlockEntity plateBE) {
                plateBE.cycleMode();
                int mode = plateBE.getCurrentMode();

                Component modeName = switch (mode) {
                    case 0 -> Component.translatable("mode.tacionian.safe").withStyle(net.minecraft.ChatFormatting.GREEN);
                    case 1 -> Component.translatable("mode.tacionian.balanced").withStyle(net.minecraft.ChatFormatting.YELLOW);
                    case 2 -> Component.translatable("mode.tacionian.performance").withStyle(net.minecraft.ChatFormatting.GOLD);
                    default -> Component.translatable("mode.tacionian.unrestricted").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD);
                };

                player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", modeName), true);
                level.playSound(null, pos, ModSounds.MODE_SWITCH.get(), SoundSource.BLOCKS, 0.6f, 0.8f + (mode * 0.1f));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StabilizationPlateBlockEntity plateBE)) return;

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                // Активуємо статус стабілізації для HUD
                energy.setPlateStabilized(true);

                // 1. Очищення дебафів (Заземлення)
                if (player.tickCount % 20 == 0) {
                    player.getActiveEffects().stream().filter(e -> !e.getEffect().isBeneficial()).findFirst().ifPresent(effect -> {
                        player.removeEffect(effect.getEffect());
                        energy.extractEnergyPure(100, false);
                        level.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, 0.5f, 1.8f);
                    });
                }

                // 2. Розрахунок межі злиття залежно від режиму плити
                int thresholdPercent = switch (plateBE.getCurrentMode()) {
                    case 0 -> 75;
                    case 1 -> 40;
                    case 2 -> 15;
                    default -> 0;
                };

                int effectiveThreshold = player.isCrouching() ? 0 : (energy.getMaxEnergy() * thresholdPercent) / 100;

                if (energy.getEnergy() > effectiveThreshold) {
                    int excess = energy.getEnergy() - effectiveThreshold;
                    int toExtract = 40 + (excess / 10);

                    // Плита намагається поглинути енергію
                    int accepted = plateBE.receiveTacionEnergy(toExtract, false);
                    energy.extractEnergyPure(toExtract, false);
                    energy.sync(player);

                    // Ефекти та звуки злиття
                    if (level.getGameTime() % 5 == 0) {
                        level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.5f);
                        if (level instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.1, 0.1, 0.1, 0.05);

                            // Якщо резервуари повні — зливаємо в повітря (Waste)
                            if (accepted < toExtract) {
                                int waste = toExtract - accepted;
                                MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, waste));
                                sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.02);
                            }
                        }
                    }
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}