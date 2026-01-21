package com.maxim.tacionian.energy.control;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyControlResolver {

    public static void resolve(ServerPlayer player, PlayerEnergy energy) {
        // 1. Стабілізатор в інвентарі (активує внутрішній таймер)
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                energy.setStabilized(true);
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = (mode == 0) ? 75 : (mode == 1) ? 40 : (mode == 2) ? 15 : 0;

                if (energy.getEnergyPercent() > threshold) {
                    energy.setRemoteNoDrain(true);
                }
                break;
            }
        }

        // 2. Стабілізаційна плита під ногами
        BlockPos pos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(pos);

        if (state.is(ModBlocks.STABILIZATION_PLATE.get())) {
            energy.setRemoteStabilized(true);
            // Присідання на плиті дозволяє скидати зайву енергію
            if (player.isCrouching() && energy.getEnergyPercent() > 5) {
                int extracted = energy.extractEnergyPure(40, false);

                if (extracted > 0) {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                            new com.maxim.tacionian.api.events.TachyonWasteEvent(player.level(), pos, extracted)
                    );

                    if (player.tickCount % 4 == 0) {
                        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY(), player.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        // 3. Загальний захист: якщо стабілізація активна, енергія не перевищує 95% (безпечний поріг)
        if ((energy.isStabilized() || energy.isRemoteStabilized()) && energy.isOverloaded()) {
            energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));
        }
    }
}