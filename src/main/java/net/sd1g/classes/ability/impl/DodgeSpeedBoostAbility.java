package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.sd1g.classes.network.ActiveTimerPayload;

import java.util.List;

public class DodgeSpeedBoostAbility implements Ability {
    @Override public String getId() { return "dodge_speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/dodge_speed_boost"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("dodge_speed_boost", 500);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1) + 1;
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200) / 20;
        int cd = getBaseCooldown() / 20;
        return "Скорость " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Быстрый откат");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1);
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200);

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, amp, false, true));
        ServerPlayNetworking.send(player, new ActiveTimerPayload("dodge_speed_boost", dur));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();

        // Шлейф у ног (облака и зеленые искры)
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 25, 0.4, 0.1, 0.4, 0.05);
        level.sendParticles(new DustParticleOptions(0x00FF00, 1.2f), x, y, z, 30, 0.5, 0.2, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.5, z, 3, 0.3, 0.1, 0.3, 0.1);

        // Звук завывания ветра
        level.playSound(null, x, y, z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.2f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.2f);

        return true;
    }
}