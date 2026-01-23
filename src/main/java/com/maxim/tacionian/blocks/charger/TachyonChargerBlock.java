package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlock extends BaseEntityBlock {
    protected final boolean isSafe;

    // Цей конструктор має бути PUBLIC, щоб ModBlocks його бачив
    public TachyonChargerBlock(BlockBehaviour.Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    // Додатковий конструктор (про всяк випадок)
    public TachyonChargerBlock(BlockBehaviour.Properties props) {
        this(props, false);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Якщо це безпечний блок, створюємо безпечне BE, інакше звичайне
        return isSafe ? new TachyonSafeChargerBlockEntity(pos, state) : new TachyonChargerBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGER_BE.get(), TachyonChargerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TachyonChargerBlockEntity charger) {

            // 1. Викачування назад (Shift + ПКМ)
            if (player.isShiftKeyDown()) {
                charger.handlePlayerExtraction(serverPlayer);
                return InteractionResult.SUCCESS;
            }

            // 2. Логіка заряджання блоку енергією гравця
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // ПЕРЕВІРКА ЛІМІТУ: 15% для безпечного, 0% для звичайного
                int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;

                if (pEnergy.getEnergy() > minLimit) {
                    int canAccept = charger.receiveTacionEnergy(100, true);
                    if (canAccept > 0) {
                        int available = pEnergy.getEnergy() - minLimit;
                        int toTransfer = Math.min(canAccept, available);

                        if (toTransfer > 0) {
                            int taken = pEnergy.extractEnergyPure(toTransfer, false);
                            charger.receiveTacionEnergy(taken, false);

                            pEnergy.addExperience(taken * 0.1f, serverPlayer);
                            pEnergy.sync(serverPlayer);

                            level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.7f, 1.4f);

                            // Виводимо прогрес: скільки зараз Tx в блоці
                            player.displayClientMessage(Component.literal("§eЗаряджання: §f" + charger.getEnergy() + " §7/ §f" + charger.getMaxCapacity() + " Tx"), true);
                        }
                    } else {
                        // Блок вже повний - просто показуємо статус
                        player.displayClientMessage(Component.literal("§bЗаряд повний: §f" + charger.getEnergy() + " Tx"), true);
                    }
                } else {
                    // Якщо енергії замало для зарядки блоку (безпека)
                    player.displayClientMessage(Component.translatable("message.tacionian.safety_limit").withStyle(ChatFormatting.RED), true);
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 1.0f, 0.5f);
                }
            });

            // Повертаємо CONSUME, щоб можна було затиснути ПКМ і зарядка йшла безперервно
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}