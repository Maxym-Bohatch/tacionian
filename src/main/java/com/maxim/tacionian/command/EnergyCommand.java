package com.maxim.tacionian.command;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

public class EnergyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tachyon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> setEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> addEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
        );
    }

    private static int setEnergy(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setEnergy(amount);
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Енергію встановлено на §f" + amount + " §7для §f" + targets.size() + " §7гравців."), true);
        return targets.size();
    }

    private static int addEnergy(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                if (amount >= 0) {
                    energy.receiveEnergy(amount, false);
                } else {
                    // ВИПРАВЛЕНО: Видалено аргумент часу (player.level().getGameTime())
                    energy.extractEnergyPure(Math.abs(amount), false);
                }
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Енергію змінено на §f" + amount + " §7для §f" + targets.size() + " §7гравців."), true);
        return targets.size();
    }

    private static void sync(ServerPlayer player, PlayerEnergy energy) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(energy));
    }
}