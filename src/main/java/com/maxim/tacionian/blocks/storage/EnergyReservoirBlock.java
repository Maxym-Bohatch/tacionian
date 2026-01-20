package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Змінено на BaseEntityBlock для зручної роботи з EntityBlock
public class EnergyReservoirBlock extends BaseEntityBlock {
    public EnergyReservoirBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyReservoirBlockEntity(pos, state);
    }

    // ВАЖЛИВО: Цей метод дозволяє методу tick у BlockEntity працювати!
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Тікер працює лише на сервері
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RESERVOIR_BE.get(),
                EnergyReservoirBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyReservoirBlockEntity reservoir) {

            // 1. Просто інформація (порожня рука + без Shift)
            if (player.getItemInHand(hand).isEmpty() && !player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                        reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }

            // 2. Взаємодія з енергією гравця
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int amount = 500;
                boolean actionHappened = false;

                if (player.isShiftKeyDown()) {
                    // Витягуємо з блоку в гравця
                    int extracted = reservoir.extractTacionEnergy(amount, false);
                    if (extracted > 0) {
                        pEnergy.receiveEnergy(extracted, false);
                        player.displayClientMessage(Component.literal("§a[<] Витягнуто: " + extracted + " Tx"), true);
                        actionHappened = true;
                    }
                } else {
                    // Заливаємо з гравця в блок
                    int taken = pEnergy.extractEnergyPure(amount, false);
                    if (taken > 0) {
                        int added = reservoir.receiveTacionEnergy(taken, false);
                        // Якщо блок прийняв менше, ніж ми взяли у гравця (наприклад, заповнився), повертаємо залишок гравцеві
                        if (added < taken) {
                            pEnergy.receiveEnergy(taken - added, false);
                        }
                        player.displayClientMessage(Component.literal("§c[>] Залито: " + added + " Tx"), true);
                        actionHappened = true;
                    }
                }

                if (actionHappened) {
                    pEnergy.sync((ServerPlayer) player);
                } else {
                    player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                            reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.RED), true);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyReservoirBlockEntity reservoir) {
                int leftover = reservoir.getEnergy();
                if (leftover > 0 && !level.isClientSide) {
                    // Вибух енергії (Waste Event) при ламанні блоку
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, leftover));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getTagElement("BlockEntityTag");
        int energy = (nbt != null) ? nbt.getInt("StoredTacion") : 0;

        tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.energy", energy, 25000).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.desc").withStyle(ChatFormatting.GRAY));
    }
}