package net.sd1g.classes.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ClassSyncPayload;
import net.sd1g.classes.network.ClassTokensSyncPayload;

import java.util.Map;
import java.util.Random;

public class ClassCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("class")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("random")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                        String[] classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                                        if (classes.length == 0) {
                                            ctx.getSource().sendFailure(Component.literal("Нет зарегистрированных классов."));
                                            return 0;
                                        }
                                        String randomClass = classes[new Random().nextInt(classes.length)];
                                        applyClass(target, randomClass);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Игроку " + target.getName().getString() + " выдан случайный класс: " + randomClass), true);
                                        return 1;
                                    })))
                    .then(Commands.literal("set")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("classId", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                for (ClassDefinition def : ClassRegistry.getAll().values()) {
                                                    builder.suggest(def.id, Component.literal("§f" + def.displayName + " §7— способностей: " + def.abilities.size()));
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                String classId = StringArgumentType.getString(ctx, "classId");
                                                ClassDefinition def = ClassRegistry.get(classId);
                                                if (def == null) {
                                                    ctx.getSource().sendFailure(Component.literal(
                                                            "Класс не найден: " + classId + ". Используйте /class list, чтобы увидеть все классы."));
                                                    return 0;
                                                }
                                                applyClass(target, classId);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "Игроку " + target.getName().getString() + " установлен класс: " + def.displayName), true);
                                                return 1;
                                            }))))
                    .then(Commands.literal("list")
                            .executes(ctx -> {
                                Map<String, ClassDefinition> all = ClassRegistry.getAll();
                                if (all.isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("Нет зарегистрированных классов."));
                                    return 0;
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("Доступные классы:"), false);
                                for (ClassDefinition def : all.values()) {
                                    ctx.getSource().sendSuccess(() -> Component.literal("§7- §f" + def.id + " §7(" + def.displayName + ")"), false);
                                }
                                return 1;
                            }))
                    .then(Commands.literal("token")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("type", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                builder.suggest("random");
                                                builder.suggest("pick3");
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                String type = StringArgumentType.getString(ctx, "type");
                                                int amount = 1;
                                                PlayerClassData data = PlayerClassData.get(target);
                                                if (type.equalsIgnoreCase("random")) {
                                                    data.randomChangeTokens += amount;
                                                    ServerPlayNetworking.send(target, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "Игроку " + target.getName().getString() + " выдан токен случайной смены класса."), true);
                                                } else if (type.equalsIgnoreCase("pick3")) {
                                                    data.pick3ChangeTokens += amount;
                                                    ServerPlayNetworking.send(target, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "Игроку " + target.getName().getString() + " выдан токен выбора из 3 классов."), true);
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("Неизвестный тип токена. Используйте 'random' или 'pick3'."));
                                                    return 0;
                                                }
                                                return 1;
                                            })
                                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        String type = StringArgumentType.getString(ctx, "type");
                                                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                        PlayerClassData data = PlayerClassData.get(target);
                                                        if (type.equalsIgnoreCase("random")) {
                                                            data.randomChangeTokens += amount;
                                                        } else if (type.equalsIgnoreCase("pick3")) {
                                                            data.pick3ChangeTokens += amount;
                                                        } else {
                                                            ctx.getSource().sendFailure(Component.literal("Неизвестный тип токена."));
                                                            return 0;
                                                        }
                                                        ServerPlayNetworking.send(target, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "Игроку " + target.getName().getString() + " выдано токенов (" + type + "): " + amount), true);
                                                        return 1;
                                                    }))))));
        });
    }

    public static void applyClass(ServerPlayer player, String classId) {
        PlayerClassData data = PlayerClassData.get(player);
        data.setPlayerClass(classId);
        PlayerClassData.markDirty(player);

        // 1. Синхронизируем HUD на клиенте
        ServerPlayNetworking.send(player, new ClassSyncPayload(classId));

        // 2. Получаем красивое имя класса
        ClassDefinition def = ClassRegistry.get(classId);
        String displayName = (def != null) ? def.displayName : classId;

        // 3. Отправляем ОДНО уведомление над хотбаром
        player.sendOverlayMessage(Component.literal("§aКласс изменён: §f" + displayName));
    }
}