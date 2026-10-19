package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ArcherAbility implements Ability {
    @Override public String getId() { return "archer_mastery"; }
    @Override public String getDisplayName() { return "Мастерство лучника"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double mult = cfg.getEffect("archer_draw_speed_multiplier", 1.5);
        double bonus = cfg.getEffect("archer_bow_damage_bonus", 3.0);
        return "Лук натягивается в " + fmt(mult) + " раза быстрее. Урон стрел +" + (int) bonus + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double mult = cfg.getEffect("archer_draw_speed_multiplier", 1.5);
        double bonus = cfg.getEffect("archer_bow_damage_bonus", 3.0);
        return List.of(
                "§7Скорость натяжения: §a×" + fmt(mult),
                "§7Бонус к урону: §c+" + (int) bonus
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Струя золотых искр у правой руки — «готовность к выстрелу»
        if (player.tickCount % 15 == 0) {
            double dx = Math.cos(Math.toRadians(player.getYRot() + 90)) * 0.4;
            double dz = Math.sin(Math.toRadians(player.getYRot() + 90)) * 0.4;
            level.sendParticles(new DustParticleOptions(0xDDA030, 0.9f),
                    x + dx, y + 1.1, z + dz, 2, 0.15, 0.1, 0.15, 0.02);
        }
        // Редкие ENCHANT — «мастерство»
        if (player.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    x, y + 1.4, z, 2, 0.4, 0.2, 0.4, 0.15);
        }
        // Столб раз в 20 тиков
        if (player.tickCount % 20 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.5;
                level.sendParticles(new DustParticleOptions(0xFFFFFF, 0.6f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }
}