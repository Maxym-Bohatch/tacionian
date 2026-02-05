package com.maxim.tacionian.command;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
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

                // --- РІВЕНЬ ТА ДОСВІД ---
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> setLevel(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "value")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("experience")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> modifyExperience(context.getSource(), EntityArgument.getPlayers(context, "targets"), (float)IntegerArgumentType.getInteger(context, "amount"), "set")))
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                                .executes(context -> modifyExperience(context.getSource(), EntityArgument.getPlayers(context, "targets"), FloatArgumentType.getFloat(context, "amount"), "add")))
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
        String langKey = mode.equals("add") ? "command.tacionian.energy.add.success" : "command.tacionian.energy.set.success";
        source.sendSuccess(() -> Component.translatable(langKey, amount, targets.size()), true);
        return targets.size();
    }

    private static int modifyExperience(CommandSourceStack source, Collection<ServerPlayer> targets, float amount, String mode) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                if (mode.equals("add")) {
                    energy.addExperience(amount, player);
                } else {
                    energy.setExperience((int) amount);
                }
                energy.sync(player);
            });
        }
        String langKey = mode.equals("add") ? "command.tacionian.experience.add.success" : "command.tacionian.experience.set.success";
        source.sendSuccess(() -> Component.translatable(langKey, amount, targets.size()), true);
        return targets.size();
    }

    private static int setLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int value) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setLevel(value);
                // Встановлюємо 50% від необхідного досвіду для нового рівня
                int halfExp = energy.getRequiredExp() / 2;
                energy.setExperience(halfExp);
                energy.sync(player);
            });
        }
        source.sendSuccess(() -> Component.translatable("command.tacionian.level.set.success", value, targets.size()), true);
        return targets.size();
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

    private static int setPushback(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setPushbackEnabled(enabled);
                energy.sync(player);
            });
        }
        Component status = Component.translatable(enabled ? "status.tacionian.active" : "status.tacionian.disabled");
        source.sendSuccess(() -> Component.translatable("command.tacionian.pushback.success", status), true);
        return targets.size();
    }

    private static int setCoreRegen(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRegenBlocked(!enabled);
                energy.sync(player);
            });
        }
        Component status = Component.translatable(enabled ? "status.tacionian.active" : "status.tacionian.disabled");
        source.sendSuccess(() -> Component.translatable("command.tacionian.regen.success", status), true);
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
        source.sendSuccess(() -> Component.translatable("command.tacionian.reset.success"), true);
        return targets.size();
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.header", target.getScoreboardName()), false);
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.level", energy.getLevel()), false);
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.energy", energy.getEnergy(), energy.getMaxEnergy()), false);

            // Тепер ми просто викликаємо метод з класу PlayerEnergy
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.exp", (int)energy.getExperience(), energy.getExperienceToNextLevel()), false);

            Component netStatus = Component.translatable(energy.isDisconnected() ? "status.tacionian.disabled" : "status.tacionian.active");
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.status", netStatus), false);

            Component pbStatus = Component.translatable(energy.isPushbackEnabled() ? "status.tacionian.active" : "status.tacionian.disabled");
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.pushback", pbStatus), false);

            Component regenStatus = Component.translatable(energy.isRegenBlocked() ? "status.tacionian.disabled" : "status.tacionian.active");
            source.sendSuccess(() -> Component.translatable("command.tacionian.info.regen", regenStatus), false);
        });
        return 1;
    }
}