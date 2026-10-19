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

import java.util.List;

public class PowerGainAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("power_gain_bonus");

    @Override public String getId() { return "power_gain"; }
    @Override public String getDisplayName() { return "Усиление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/power_gain"); }

    // Жесткая формула из документа
    public static double getBonusHP(int level) {
        double hp = 0;

        // 1-100 уровень: +1 HP за каждые 10 уровней
        if (level >= 10) {
            hp += Math.min(level / 10, 10) * 1.0;
        }
        // 101-200 уровень: +0.5 HP за каждые 10 уровней
        if (level > 100) {
            hp += Math.min((level - 100) / 10, 10) * 0.5;
        }
        // 201+ уровень: +0.25 HP за каждые 10 уровней
        if (level > 200) {
            hp += ((level - 200) / 10) * 0.25;
        }

        return hp;
    }

    @Override
    public String getDescription() {
        return "За каждые 10 уровней опыта получает +1 НР урона в ближнем бою; после 100-го уровня +0,5 НР, после 200-го +0,25 НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int level = player.experienceLevel;
        double bonusHP = getBonusHP(level);
        int toNext = 10 - (level % 10);

        return List.of(
                "§7Текущий уровень: §f" + level,
                "§7Текущий бонус: §c+" + String.format("%.2f", bonusHP) + " НР урона",
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double bonusHP = getBonusHP(player.experienceLevel);
        // В Minecraft 1 HP = 0.5 единицы урона в атрибутах
        double targetDamage = bonusHP * 0.5;

        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;
        boolean increased = targetDamage > oldBonus + 1e-6;
        boolean changed = Math.abs(oldBonus - targetDamage) > 1e-6;

        if (changed) {
            if (targetDamage > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER_ID, targetDamage, AttributeModifier.Operation.ADD_VALUE));
            } else {
                attr.removeModifier(MODIFIER_ID);
            }
        }

        // Эффекты при повышении уровня
        if (increased) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.ANVIL_FALL, SoundSource.PLAYERS, 0.9f, 1.4f);
            level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.3f), player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }

        // Аура (красные искры)
        if (player.tickCount % 15 == 0) {
            ServerLevel level = player.level();
            double x = player.getX(), y = player.getY(), z = player.getZ();
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.0f), x + 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
            level.sendParticles(new DustParticleOptions(0xCC2222, 1.0f), x - 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
        }
    }

    public static void removeBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}