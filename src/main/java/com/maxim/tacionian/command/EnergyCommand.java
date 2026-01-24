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
                        .then(Commands.literal("remove")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> modifyEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "remove")))
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

                // --- ДОСВІД ---
                .then(Commands.literal("experience")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> modifyExp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "add")))
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> modifyExp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), "set")))
                                )
                        )
                )

                // --- ІНФО ---
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showInfo(context.getSource(), EntityArgument.getPlayer(context, "target")))
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
                        .then(Commands.literal("core_regen")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setCoreRegen(context.getSource(), EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled")))
                                        )
                                )
                        )
                )

                // --- СКИНУТИ ---
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
                int newVal = switch (mode) {
                    case "add" -> energy.getEnergy() + amount;
                    case "remove" -> energy.getEnergy() - amount;
                    default -> amount;
                };
                energy.setEnergy(newVal);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.translatable("command.tacionian.energy." + mode + ".success", amount, targets.size()), true);
        return targets.size();
    }

    private static int setLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int value) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setLevel(value);
                energy.setExperience(0);
                energy.setEnergy(energy.getMaxEnergy() / 2);
                energy.sync(player);
            });
        }
        // Фікс відображення: передаємо значення рівня першим, кількість гравців другим
        source.sendSuccess(() -> Component.translatable("command.tacionian.level.set.success", value, targets.size()), true);
        return targets.size();
    }

    private static int modifyExp(CommandSourceStack source, Collection<ServerPlayer> targets, int amount, String mode) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                if (mode.equals("add")) {
                    energy.addExperience((float) amount, player);
                } else {
                    energy.setExperience(amount);
                }
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.translatable("command.tacionian.experience." + mode + ".success", amount, targets.size()), true);
        return targets.size();
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.header", target.getDisplayName()), false);
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.level", energy.getLevel()), false);
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.energy", energy.getEnergy(), energy.getMaxEnergy()), false);
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.exp", energy.getExperience(), energy.getRequiredExp()), false);

            Component status = Component.translatable(energy.isDisconnected() ? "status.tacionian.disabled" : "status.tacionian.active");
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.status", status), false);
        });
        return 1;
    }

    private static int setNetworkState(CommandSourceStack source, Collection<ServerPlayer> targets, boolean active) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setDisconnected(!active);
                energy.sync(player);
            });
        }
        Component status = Component.translatable(active ? "status.tacionian.active" : "status.tacionian.disabled");
        source.sendSuccess(() -> Component.translatable("command.tacionian.network.success", status), true);
        return targets.size();
    }

    private static int setCoreRegen(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRegenBlocked(!enabled);
                energy.sync(player);
            });
        }
        Component status = Component.translatable(enabled ? "status.tacionian.active" : "status.tacionian.blocked");
        source.sendSuccess(() -> Component.translatable("command.tacionian.regen.success", status), true);
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