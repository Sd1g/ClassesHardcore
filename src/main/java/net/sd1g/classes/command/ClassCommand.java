package net.sd1g.classes.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.ability.impl.*;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ClassSyncPayload;
import net.sd1g.classes.network.ClassTokensSyncPayload;
import net.sd1g.classes.network.CooldownSyncPayload;
import net.sd1g.classes.network.HeroSyncPayload;
import net.sd1g.classes.network.LuckyDiggerSyncPayload;

import java.util.ArrayList;
import java.util.List;
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
                                                "Игроку " + target.getName().getString()
                                                        + " выдан случайный класс: " + randomClass), true);
                                        return 1;
                                    })))

                    .then(Commands.literal("set")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("classId", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                for (ClassDefinition def : ClassRegistry.getAll().values()) {
                                                    builder.suggest(
                                                            def.id,
                                                            Component.literal("§f" + def.displayName
                                                                    + " §7— способностей: " + def.abilities.size())
                                                    );
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                String classId = StringArgumentType.getString(ctx, "classId");
                                                ClassDefinition def = ClassRegistry.get(classId);
                                                if (def == null) {
                                                    ctx.getSource().sendFailure(Component.literal(
                                                            "Класс не найден: " + classId
                                                                    + ". Используйте /class list, чтобы увидеть все классы."));
                                                    return 0;
                                                }
                                                applyClass(target, classId);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "Игроку " + target.getName().getString()
                                                                + " установлен класс: " + def.displayName), true);
                                                return 1;
                                            }))))

                    .then(Commands.literal("list")
                            .executes(ctx -> {
                                Map<String, ClassDefinition> all = ClassRegistry.getAll();
                                if (all.isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("Нет зарегистрированных классов."));
                                    return 0;
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§eСписок классов (§7всего: " + all.size() + "§e):"), false);
                                for (ClassDefinition def : all.values()) {
                                    int passive = 0;
                                    int active = 0;
                                    for (var ability : def.abilities) {
                                        if (ability.isPassive()) passive++;
                                        else active++;
                                    }
                                    final int p = passive;
                                    final int a = active;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            " §7- §f" + def.id + " §7(§f" + def.displayName + "§7) §7[активных: "
                                                    + a + ", пассивных: " + p + "]"), false);
                                }
                                return all.size();
                            }))

                    .then(Commands.literal("cooldown")
                            .then(Commands.literal("reset")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                int count = resetActiveCooldowns(target);
                                                if (count == 0) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§7У игрока §f" + target.getName().getString()
                                                                    + " §7нет активных кулдаунов."), true);
                                                    return 0;
                                                }
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§aУ игрока §f" + target.getName().getString()
                                                                + " §aсброшено кулдаунов: §f" + count + "§a."), true);
                                                return count;
                                            }))))

                    // ============= /class token =============
                    .then(Commands.literal("token")
                            .then(Commands.literal("random")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                PlayerClassData data = PlayerClassData.get(target);
                                                data.randomChangeTokens++;
                                                PlayerClassData.markDirty(target);
                                                ServerPlayNetworking.send(target, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§aВыдан 1 токен на случайную смену класса игроку §f" + target.getName().getString()), true);
                                                return 1;
                                            })))
                            .then(Commands.literal("pick3")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                PlayerClassData data = PlayerClassData.get(target);
                                                data.pick3ChangeTokens++;
                                                PlayerClassData.markDirty(target);
                                                ServerPlayNetworking.send(target, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§aВыдан 1 токен на выбор из 3 классов игроку §f" + target.getName().getString()), true);
                                                return 1;
                                            })))));
        });
    }

    private static int resetActiveCooldowns(ServerPlayer target) {
        PlayerClassData data = PlayerClassData.get(target);
        long now = target.level().getServer() != null
                ? target.level().getServer().getTickCount()
                : 0L;

        List<String> activeIds = new ArrayList<>();
        for (Map.Entry<String, Long> e : data.abilityCooldowns.entrySet()) {
            if (e.getValue() != null && e.getValue() > now) {
                activeIds.add(e.getKey());
            }
        }

        if (activeIds.isEmpty()) {
            data.abilityCooldowns.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= now);
            return 0;
        }

        for (String id : activeIds) {
            data.abilityCooldowns.remove(id);
            ServerPlayNetworking.send(target, new CooldownSyncPayload(id, 0));
        }
        PlayerClassData.markDirty(target);
        return activeIds.size();
    }

    public static void applyClass(ServerPlayer target, String classId) {
        PowerGainAbility.removeBonus(target);
        SpeedBoostAbility.removeEffects(target);
        LuckyDiggerMiningAbility.removeEffects(target);
        VitalityAbility.removeEffects(target);
        LifeExperienceAbility.removeEffects(target);
        IceStrikeAbility.removeBonus(target);
        VampireDrainAbility.cancelIfActive(target);
        InvisibilityAbility.cancelIfActive(target);
        RunnerSprintAbility.cancelIfActive(target);
        RunnerPassiveAbility.removeEffects(target);
        HeroPassiveAbility.removeEffects(target);
        PorcupineReflectAbility.clearTracking(target.getUUID());
        PorcupineThornsAbility.clearTracking(target.getUUID());
        AssassinAbility.removeBonus(target);
        TitanAbility.removeEffects(target);

        PlayerClassData data = PlayerClassData.get(target);
        data.setPlayerClass(classId);
        data.gluttonyDepletionArmed = false;
        data.invisibleAmbushArmed = false;
        data.bruteShakeArmed = false;
        data.climbingActive = false;
        data.climbingInProgress = false;

        if ("hero".equals(classId)) {
            try {
                Stat<Identifier> stat = Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER);
                data.heroTradeBaseline = target.getStats().getValue(stat);
            } catch (Exception e) {
                data.heroTradeBaseline = 0;
            }
            data.heroTrades = 0;
        } else {
            data.heroTrades = 0;
            data.heroTradeBaseline = 0;
        }

        PlayerClassData.markDirty(target);
        ServerPlayNetworking.send(target, new ClassSyncPayload(classId));
        ServerPlayNetworking.send(target, new HeroSyncPayload(data.heroTrades));

        if ("lucky_digger".equals(classId)) {
            ServerPlayNetworking.send(target, new LuckyDiggerSyncPayload(
                    data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
        }
    }
}