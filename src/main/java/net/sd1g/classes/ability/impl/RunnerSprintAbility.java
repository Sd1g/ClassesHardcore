package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.RunnerSyncPayload;

import java.util.List;

public class RunnerSprintAbility implements Ability {
    @Override public String getId() { return "runner_sprint"; }
    @Override public String getDisplayName() { return "Рывок"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 800; } // 40 сек

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_sprint"); }

    @Override
    public String getDescription() {
        return "Получает Скорость V на 15 секунд. Перезарядка: 40 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект: §bСкорость V", "§7Длительность: §f15 сек.", "§7Кулдаун: §f40 сек.");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.runnerSprintTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.runnerSprintTicks = 300; // 15 сек
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 4, false, false, true)); // amp 4 = Speed V
        ServerPlayNetworking.send(player, new RunnerSyncPayload(300));

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.5f, 1.2f);
        player.sendOverlayMessage(Component.literal("§bРывок. §fСкорость V на 15 сек."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks <= 0) return;

        data.runnerSprintTicks--;

        if (data.runnerSprintTicks <= 0) {
            data.runnerSprintTicks = 0;
            RunnerPassiveAbility.applyPassiveSpeed(player);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new RunnerSyncPayload(0));
            player.sendOverlayMessage(Component.literal("§bРывок закончился"));
        }
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) {
            data.runnerSprintTicks = 0;
            player.removeEffect(MobEffects.SPEED);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new RunnerSyncPayload(0));
        }
    }
}