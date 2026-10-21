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
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class RunnerPassiveAbility implements Ability {
    @Override public String getId() { return "runner_passive"; }
    @Override public String getDisplayName() { return "Разгон"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_passive"); }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1) + 1;
        return "Постоянно даёт Скорость " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Пассивный эффект");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) return;

        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);

        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
        }

        ServerLevel level = player.level();

        // Ненавязчивый эффект у ног при беге
        if (player.isSprinting() && player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.1, 0.0, 0.1, 0.01);
        }

        // Тихий звук шагов/ветра (раз в 1.5 секунды)
        if (player.tickCount % 30 == 0 && player.isSprinting()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.2f, 1.5f);
        }
    }

    public static void applyPassiveSpeed(ServerPlayer player) {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
    }
}