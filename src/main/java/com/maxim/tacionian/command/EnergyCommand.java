/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.command;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class EnergyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tachyon")
                .requires(source -> source.hasPermission(2))

                // --- ЕНЕРГІЯ ---
                .then(Commands.literal("energy")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "set")))
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "add")))
                                )
                        )
                )

                // --- РІВЕНЬ ---
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> setLevel(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "value")))
                                        )
                                )
                        )
                )

                // --- СТАНИ ---
                .then(Commands.literal("state")
                        .then(Commands.literal("network")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("active", BoolArgumentType.bool())
                                                .executes(context -> setNetworkState(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "active")))
                                        )
                                )
                        )
                        .then(Commands.literal("wireless")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("visible", BoolArgumentType.bool())
                                                .executes(context -> setWirelessVisibility(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "visible")))
                                        )
                                )
                        )
                        .then(Commands.literal("pushback")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setPushback(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled")))
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

                // --- ІНФО ТА СКИНУТИ ---
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showInfo(context.getSource(), EntityArgument.getPlayer(context, "target")))
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
                if (mode.equals("add")) energy.setEnergy(energy.getEnergy() + amount);
                else energy.setEnergy(amount);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("Energy " + mode + " for " + targets.size() + " players."), true);
        return targets.size();
    }

    private static int setLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int value) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setLevel(value);
                energy.setExperience(0);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("Level set to " + value + " for " + targets.size() + " players."), true);
        return targets.size();
    }

    private static int setWirelessVisibility(CommandSourceStack source, Collection<ServerPlayer> targets, boolean visible) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRemoteAccessBlocked(!visible);
                energy.sync(player);
            });
        }
        String status = visible ? "VISIBLE" : "STEALTH";
        source.sendSuccess(() -> Component.literal("Wireless visibility: " + status), true);
        return targets.size();
    }

    private static int setPushback(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setPushbackEnabled(enabled);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("Pushback effect enabled: " + enabled), true);
        return targets.size();
    }

    private static int setNetworkState(CommandSourceStack source, Collection<ServerPlayer> targets, boolean active) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setDisconnected(!active);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("Network active: " + active), true);
        return targets.size();
    }

    private static int setCoreRegen(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRegenBlocked(!enabled);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("Core regen enabled: " + enabled), true);
        return targets.size();
    }

    private static int resetStats(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setLevel(1);
                energy.setEnergy(0);
                energy.setExperience(0);
                energy.setDisconnected(false);
                energy.setRemoteAccessBlocked(false);
                energy.setPushbackEnabled(true);
                energy.setRegenBlocked(false);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.literal("All stats reset for selected players."), true);
        return targets.size();
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
            source.sendSuccess(() -> Component.literal("§b--- " + target.getScoreboardName() + " Tachyon Stats ---"), false);
            source.sendSuccess(() -> Component.literal("Level: " + energy.getLevel()), false);
            source.sendSuccess(() -> Component.literal("Energy: " + energy.getEnergy() + "/" + energy.getMaxEnergy()), false);
            source.sendSuccess(() -> Component.literal("Stealth: " + energy.isRemoteAccessBlocked()), false);
            source.sendSuccess(() -> Component.literal("Pushback: " + energy.isPushbackEnabled()), false);
        });
        return 1;
    }
}