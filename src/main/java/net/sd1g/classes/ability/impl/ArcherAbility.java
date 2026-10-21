package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ArcherAbility implements Ability {
    @Override public String getId() { return "archer_mastery"; }
    @Override public String getDisplayName() { return "Мастерство лучника"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/archer_mastery"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double mult = cfg.getEffect("archer_draw_speed_multiplier", 1.5);
        double dmg = cfg.getEffect("archer_pure_damage", 4.0) / 2.0;
        return "Лук натягивается в " + mult + " раза быстрее. Полный выстрел наносит +" + dmg + " НР чистого урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Кулдаун чистого урона: 1.5 сек.");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();

        // Аура скорости и лука (частицы)
        if (player.tickCount % 5 == 0) {
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            double angle = (player.tickCount * 0.15) % (Math.PI * 2);
            double radius = 0.8;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.ENCHANT, px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.0f), x, y, z, 1, 0.5, 0.2, 0.5, 0.02);
        }

        // ИСПРАВЛЕНИЕ: BOW_PULL -> CROSSBOW_LOADING_MIDDLE (тихий скрип тетивы)
        ModConfig cfg = ModConfig.getInstance();
        int soundInterval = Math.max(1, cfg.getIntEffect("archer_aura_sound_interval", 20));

        if (player.tickCount % soundInterval == 0 && player.isSprinting()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.15f, 1.8f);
        }
    }
}