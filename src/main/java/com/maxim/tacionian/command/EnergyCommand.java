package com.maxim.tacionian.command;

import com.maxim.tacionian.config.TacionianConfig;
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
                // ЕНЕРГІЯ
                .then(Commands.literal("energy")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setEnergy(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                )
                // РІВЕНЬ
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        // ВИПРАВЛЕНО: Змінено ліміт з 10 на 100
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, TacionianConfig.MAX_LEVEL.get()))
                                                .executes(context -> setLevel(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "level")))
                                        )
                                )
                        )
                )
                // ДОСВІД (EXP)
                .then(Commands.literal("exp")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> addExp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setExp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                )
                // RESET
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> resetStats(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                        )
                )
        );
    }

    // ... (setEnergy залишається без змін)

    private static int setLevel(CommandSourceStack source, Collection<ServerPlayer> targets, int level) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                // Встановлюємо новий рівень
                energy.setLevel(level);

                // Скидаємо досвід та дробовий досвід в нуль для чистоти рівня
                energy.setExperience(0);
                // Якщо у тебе в PlayerEnergy є доступ до fractionalExperience,
                // можна додати метод для його скидання, але зазвичай вистачає Experience

                // Одразу заповнюємо енергію до нового максимуму
                energy.setEnergy(energy.getMaxEnergy());

                // Синхронізуємо з клієнтом
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Рівень встановлено на §6" + level + " §7(Енергія заповнена)"), true);
        return targets.size();
    }

    private static int setEnergy(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setEnergy(amount);
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Енергію встановлено на §f" + amount), true);
        return targets.size();
    }

    private static int addExp(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.addExperience((float) amount, player);
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Додано §e" + amount + " §7досвіду."), true);
        return targets.size();
    }

    private static int setExp(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setExperience(amount);
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Досвід встановлено на §e" + amount), true);
        return targets.size();
    }

    private static int resetStats(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setEnergy(0);
                energy.setLevel(1);
                energy.setExperience(0);
                sync(player, energy);
            });
        }
        source.sendSuccess(() -> Component.literal("§b[Tacionian] §7Всі показники скинуто."), true);
        return targets.size();
    }

    private static void sync(ServerPlayer player, PlayerEnergy energy) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(energy));
    }
}