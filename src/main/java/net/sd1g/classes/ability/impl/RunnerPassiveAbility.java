package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class RunnerPassiveAbility implements Ability {
    @Override public String getId() { return "runner_passive"; }
    @Override public String getDisplayName() { return "Разгон"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_passive"); }

    @Override
    public String getDescription() {
        return "Постоянно получает Скорость II.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект: §bСкорость II §7(постоянно)");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) return; // Не перебиваем Рывок

        int amplifier = 1; // Speed II
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != amplifier;
        boolean lowDuration = current != null && current.getDuration() < 40;

        if (wasAbsent || wrongAmp || lowDuration) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amplifier, true, false));
        }

        if (wasAbsent) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.6f, 1.5f);
        }

        if (player.tickCount % 8 == 0) {
            ServerLevel level = player.level();
            level.sendParticles(new DustParticleOptions(0x4D99FF, 1.0f), player.getX(), player.getY() + 0.05, player.getZ(), 3, 0.35, 0.03, 0.35, 0.02);
        }
    }

    public static void applyPassiveSpeed(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1, true, false));
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.SPEED);
    }
}