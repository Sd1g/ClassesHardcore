package net.sd1g.classes.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ClassSyncPayload;
import net.sd1g.classes.network.ClassTokensSyncPayload;
import net.sd1g.classes.network.CooldownSyncPayload;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ClassCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("class")
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
                // === ВОЗВРАЩЕННАЯ КОМАНДА СБРОСА КУЛДАУНОВ ===
                .then(Commands.literal("resetcd")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    resetCooldowns(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Все кулдауны игрока " + target.getName().getString() + " сброшены."), true);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            // Если игрок не указан, сбрасываем кулдаун тому, кто ввел команду
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                resetCooldowns(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Ваши кулдауны сброшены."), false);
                                return 1;
                            }
                            ctx.getSource().sendFailure(Component.literal("Укажите игрока: /class resetcd <player>"));
                            return 0;
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
                                                })))))));
    }

    private static void resetCooldowns(ServerPlayer target) {
        PlayerClassData data = PlayerClassData.get(target);

        // Очищаем кулдауны способностей
        data.abilityCooldowns.clear();

        // Сбрасываем кулдаун кристаллов
        data.crystalCooldownEndTick = 0;
        data.endCrystalUses = 0;

        PlayerClassData.markDirty(target);

        // Синхронизируем с клиентом, чтобы HUD сразу обновился
        ClassDefinition def = data.getClassDef();
        if (def != null) {
            for (Ability a : def.abilities) {
                ServerPlayNetworking.send(target, new CooldownSyncPayload(a.getId(), 0));
            }
        }
        ServerPlayNetworking.send(target, new CooldownSyncPayload("end_crystal", 0));
        ServerPlayNetworking.send(target, new CooldownSyncPayload("teleport", 0));
        ServerPlayNetworking.send(target, new CooldownSyncPayload("teleporter_count", data.teleporterCount));
    }

    public static void applyClass(ServerPlayer player, String newClassId) {
        PlayerClassData data = PlayerClassData.get(player);
        String oldClassId = data.playerClass;

        // 1. Принудительно снимаем базовые эффекты
        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.ABSORPTION);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.JUMP_BOOST);
        player.removeEffect(MobEffects.LUCK);
        player.removeEffect(MobEffects.RESISTANCE);

        // 2. Вычищаем ВСЕ атрибуты (сердца, урон, скорость), которые наложил наш мод
        removeMods(player, Attributes.MAX_HEALTH);
        removeMods(player, Attributes.ATTACK_DAMAGE);
        removeMods(player, Attributes.ATTACK_SPEED);
        removeMods(player, Attributes.MOVEMENT_SPEED);
        removeMods(player, Attributes.ARMOR);
        removeMods(player, Attributes.ARMOR_TOUGHNESS);

        // 3. Даем способностям старого класса очистить свои внутренние стейты
        ClassDefinition oldDef = ClassRegistry.get(oldClassId);
        if (oldDef != null) {
            for (Ability ability : oldDef.abilities) {
                ability.onRemove(player);
            }
        }

        // 4. Устанавливаем новый класс
        data.setPlayerClass(newClassId);
        PlayerClassData.markDirty(player);

        // 5. Синхронизация с клиентом
        ServerPlayNetworking.send(player, new ClassSyncPayload(newClassId));

        ClassDefinition def = ClassRegistry.get(newClassId);
        String displayName = (def != null) ? def.displayName : newClassId;
        player.sendOverlayMessage(Component.literal("§aКласс изменён: §f" + displayName));
    }

    // Вспомогательный метод для удаления модификаторов (исправляет ошибку Holder)
    private static void removeMods(ServerPlayer player, Holder<Attribute> attrHolder) {
        AttributeInstance inst = player.getAttribute(attrHolder);
        if (inst != null) {
            Set<AttributeModifier> toRemove = new HashSet<>();
            for (AttributeModifier mod : inst.getModifiers()) {
                if (mod.id().getNamespace().equals(ClassesHardcore.MOD_ID)) {
                    toRemove.add(mod);
                }
            }
            for (AttributeModifier mod : toRemove) {
                inst.removeModifier(mod);
            }
        }
    }
}