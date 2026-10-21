package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
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
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class SpeedBoostAbility implements Ability {
    @Override public String getId() { return "speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return true; }

    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/speed_boost"); }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0) + 1;
        return "Постоянно даёт Скорость " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0) + 1;
        return List.of("§7Эффект: §bСкорость " + amp);
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0);
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);
        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
        }

        ServerLevel level = player.level();

        // Частицы ветра у ног
        if (player.tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.0, 0.2, 0.02);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.1, 0.0, 0.1, 0.0);
        }

        // Звук быстрого бега (только при спринте)
        if (player.tickCount % 12 == 0 && player.isSprinting()) {
            // ИСПРАВЛЕНИЕ: BLOCK_GRASS_STEP -> GRASS_STEP
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_STEP, SoundSource.PLAYERS, 0.4f, 1.8f);
            // Очень тихий звук рассекаемого воздуха
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.15f, 1.5f);
        }
    }
}