package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class LifeExperienceAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("life_experience_bonus_hp");

    @Override public String getId() { return "life_experience"; }
    @Override public String getDisplayName() { return "Жизненный опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int per = cfg.getIntEffect("life_exp_levels_per_point", 5);
        int hp = cfg.getIntEffect("life_exp_bonus_hp_per_point", 2);
        return "За каждые " + per + " уровней опыта +" + hp + " HP к максимуму.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int per = cfg.getIntEffect("life_exp_levels_per_point", 5);
        int hpPerPoint = cfg.getIntEffect("life_exp_bonus_hp_per_point", 2);
        int level = player.experienceLevel;
        int points = per > 0 ? level / per : 0;
        int bonus = points * hpPerPoint;
        int toNext = per > 0 ? per - (level % per) : 0;
        return List.of(
                "§7Текущий бонус: §c+" + bonus + " HP",
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int per = cfg.getIntEffect("life_exp_levels_per_point", 5);
        int hpPerPoint = cfg.getIntEffect("life_exp_bonus_hp_per_point", 2);
        int points = per > 0 ? player.experienceLevel / per : 0;
        double targetBonus = (double) points * hpPerPoint;

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;
        boolean changed = Math.abs(oldBonus - targetBonus) > 1e-6;
        boolean increased = targetBonus > oldBonus + 1e-6;

        if (changed) {
            if (targetBonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER_ID, targetBonus, AttributeModifier.Operation.ADD_VALUE));
            } else if (existing != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }

        // Событие повышения HP — звук + вспышка
        if (increased) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 1.0f, 1.4f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0f, 1.6f);

            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 25, 0.6, 0.7, 0.6, 0.4);
            level.sendParticles(new DustParticleOptions(0xB34DFF, 1.4f), x, y, z, 18, 0.5, 0.5, 0.5, 0.15);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.5, 0.5, 0.5, 0.05);
        }

        // Циклическая аура — фиолетовая спираль
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Столб фиолетовых частиц — раз в 12 тиков
        if (player.tickCount % 12 == 0) {
            double angle = (player.tickCount * 0.25) % (Math.PI * 2);
            for (int i = 0; i < 3; i++) {
                double dy = 0.3 + i * 0.5;
                double r = 0.5 + i * 0.1;
                double px = x + Math.cos(angle + i) * r;
                double pz = z + Math.sin(angle + i) * r;
                level.sendParticles(new DustParticleOptions(0xB34DFF, 0.9f),
                        px, y + dy, pz, 1, 0, 0.01, 0, 0);
            }
        }
        // ENCHANT высоко над головой — раз в 25 тиков
        if (player.tickCount % 25 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.6, z, 3, 0.5, 0.3, 0.5, 0.15);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}