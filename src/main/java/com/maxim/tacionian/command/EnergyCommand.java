package com.maxim.tacionian.command;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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

                .then(Commands.literal("energy")
                        // Встановити конкретне значення
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "set")))
                                )
                        )
                        // Додати енергію
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "add")))
                                )
                        )
                        // Відняти енергію
                        .then(Commands.literal("remove")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "remove")))
                                )
                        )
                )
                .then(Commands.literal("state")
                        .then(Commands.literal("network")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("active", BoolArgumentType.bool())
                                                .executes(context -> setNetworkState(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "active")))
                                        )
                                )
                        )
                        .then(Commands.literal("core_regen")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setCoreRegen(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> resetStats(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                        )
                )
        );
    }

    private static int modifyEnergy(CommandSourceStack source, Collection<ServerPlayer> targets, int amount, String mode) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                int newVal;
                switch (mode) {
                    case "add" -> newVal = energy.getEnergy() + amount;
                    case "remove" -> newVal = energy.getEnergy() - amount;
                    default -> newVal = amount; // "set"
                }
                energy.setEnergy(newVal);
                energy.sync(player);
            });
        }

        String translationKey = "command.tacionian.energy." + mode + ".success";
        source.sendSuccess(() -> Component.translatable(translationKey, amount, targets.size()), true);
        return targets.size();
    }

    private static int setNetworkState(CommandSourceStack source, Collection<ServerPlayer> targets, boolean active) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setDisconnected(!active);
                energy.sync(player);
            });
        }
        String key = active ? "status.tacionian.active" : "status.tacionian.disabled";
        source.sendSuccess(() -> Component.translatable("command.tacionian.network.success", Component.translatable(key)), true);
        return targets.size();
    }

    private static int setCoreRegen(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRegenBlocked(!enabled);
                energy.sync(player);
            });
        }
        String key = enabled ? "status.tacionian.active" : "status.tacionian.blocked";
        source.sendSuccess(() -> Component.translatable("command.tacionian.regen.success", Component.translatable(key)), true);
        return targets.size();
    }

    private static int resetStats(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setEnergy(0);
                energy.setLevel(1);
                energy.setExperience(0);
                energy.setDisconnected(false);
                energy.setRegenBlocked(false);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.translatable("command.tacionian.reset.success"), true);
        return targets.size();
    }
}