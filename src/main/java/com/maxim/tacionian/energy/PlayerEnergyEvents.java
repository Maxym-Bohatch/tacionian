package com.maxim.tacionian.energy;

import com.maxim.tacionian.energy.control.EnergyControlResolver;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "tacionian", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEnergyEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation("tacionian", "player_energy"), new PlayerEnergyProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.player instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                long currentTime = player.level().getGameTime();

                if (event.phase == TickEvent.Phase.START) {
                    // ВИКЛИК ТІКУ: Регенерація та автоматичне виправлення рівня
                    energy.tick(player);

                    // Скидання статусів та пасивний захист
                    EnergyControlResolver.resolve(player, energy);

                } else { // Phase.END
                    // АКТИВНИЙ СТАБІЛІЗАТОР (ПКМ)
                    if (player.isUsingItem() && player.getUseItem().getItem() instanceof EnergyStabilizerItem) {
                        int drained = energy.extractEnergyPure(20, false, currentTime);

                        if (drained > 0) {
                            if (player.tickCount % 2 == 0) {
                                player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                        player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
                            }
                            if (player.tickCount % 10 == 0) {
                                player.level().playSound(null, player.blockPosition(),
                                        net.minecraft.sounds.SoundEvents.BEE_LOOP, net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 1.4F);
                            }
                        }
                    }

                    // Застосування негативних/позитивних ефектів
                    PlayerEnergyEffects.apply(player, energy);

                    // Синхронізація з клієнтом (кожні 2 тики)
                    if (player.tickCount % 2 == 0) {
                        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(energy));
                    }
                }
            });
        }
    }
}