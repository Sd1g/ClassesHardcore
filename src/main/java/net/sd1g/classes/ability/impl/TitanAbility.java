package net.sd1g.classes.ability.impl;

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

import java.util.List;

public class TitanAbility implements Ability {
    @Override public String getId() { return "titan_passive"; }
    @Override public String getDisplayName() { return "Каменная кожа"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/titan_passive"); }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 0) + 1;
        return "Постоянно дает Сопротивление " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Пассивный эффект");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("titan_resistance_amplifier", 0);
        MobEffectInstance current = player.getEffect(MobEffects.RESISTANCE);
        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, amp, true, false));
        }

        ServerLevel level = player.level();

        // Аура из пыли
        int particleInterval = Math.max(1, cfg.getIntEffect("titan_aura_particle_interval", 5));
        if (player.tickCount % particleInterval == 0) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5;
            double y = player.getY() + 0.1 + player.getRandom().nextDouble() * 1.5;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5;

            level.sendParticles(new DustParticleOptions(0x808080, 1.0f), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.ASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Тихий звук камней
        int soundInterval = Math.max(1, cfg.getIntEffect("titan_aura_sound_interval", 80));
        if (player.tickCount % soundInterval == 0) {
            // ИСПРАВЛЕНИЕ: BLOCK_STONE_STEP -> STONE_STEP
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.3f, 0.8f);
        }
    }
}