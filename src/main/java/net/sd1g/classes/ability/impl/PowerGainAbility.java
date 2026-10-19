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

public class PowerGainAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("power_gain_bonus");

    @Override public String getId() { return "power_gain"; }
    @Override public String getDisplayName() { return "Усиление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    public static double computeBonus(int level) {
        ModConfig cfg = ModConfig.getInstance();
        int levelsPerPoint = cfg.getIntEffect("power_gain_levels_per_point", 10);
        if (levelsPerPoint <= 0) return 0;
        double t1 = cfg.getEffect("power_gain_damage_tier1", 0.5);
        double t2 = cfg.getEffect("power_gain_damage_tier2", 0.25);
        double t3 = cfg.getEffect("power_gain_damage_tier3", 0.1);
        int maxT1 = cfg.getIntEffect("power_gain_tier1_max_level", 100);
        int maxT2 = cfg.getIntEffect("power_gain_tier2_max_level", 200);

        double bonus = 0;
        bonus += (Math.min(level, maxT1) / (double) levelsPerPoint) * t1;
        if (level > maxT1) bonus += (Math.min(level - maxT1, maxT2 - maxT1) / (double) levelsPerPoint) * t2;
        if (level > maxT2) bonus += ((level - maxT2) / (double) levelsPerPoint) * t3;
        return bonus;
    }

    @Override
    public String getDescription() {
        int levels = ModConfig.getInstance().getIntEffect("power_gain_levels_per_point", 10);
        return "Даёт бонус к урону за каждые " + levels + " уровней опыта.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int levelsPerPoint = cfg.getIntEffect("power_gain_levels_per_point", 10);
        int level = player.experienceLevel;
        double bonus = computeBonus(level);
        int toNext = levelsPerPoint > 0 ? levelsPerPoint - (level % levelsPerPoint) : 0;
        return List.of(
                "§7Текущий уровень: §f" + level,
                "§7Текущий бонус: §c+" + fmt(bonus) + " урона",
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double targetBonus = computeBonus(player.experienceLevel);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;
        boolean increased = targetBonus > oldBonus + 1e-6;
        boolean changed = Math.abs(oldBonus - targetBonus) > 1e-6;

        if (changed) {
            if (targetBonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER_ID, targetBonus, AttributeModifier.Operation.ADD_VALUE));
            } else {
                attr.removeModifier(MODIFIER_ID);
            }
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Событие повышения — звук + вспышка
        if (increased) {
            level.playSound(null, x, y + 1, z, SoundEvents.ANVIL_FALL, SoundSource.PLAYERS, 0.9f, 1.4f);
            level.playSound(null, x, y + 1, z, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 0.8f, 1.6f);
            level.sendParticles(ParticleTypes.CRIT, x, y + 1, z, 15, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(ParticleTypes.FLAME, x, y + 1, z, 10, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.3f), x, y + 1, z, 20, 0.5, 0.5, 0.5, 0.05);
            level.sendParticles(new DustParticleOptions(0xFF6619, 1.1f), x, y + 1, z, 15, 0.4, 0.4, 0.4, 0.05);
        }

        // Аура багровая — искры у рук, раз в 15 тиков
        if (player.tickCount % 15 == 0) {
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.0f),
                    x + 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.0f),
                    x - 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
        }
        // Столб багровых частиц — восходит, раз в 15 тиков
        if (player.tickCount % 15 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0xCC2222, 0.9f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        // Редкие CRIT — «искры силы»
        if (player.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.CRIT, x, y + 1.2, z, 3, 0.4, 0.3, 0.4, 0.05);
        }
    }

    public static void removeBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}