package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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

    public static double computeBonus(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int level = player.experienceLevel;
        int levelsPerStep = cfg.getIntEffect("strongman_levels_per_step", 10);
        if (levelsPerStep <= 0) return 0;

        double t1 = cfg.getEffect("strongman_tier1_dmg", 1.0);
        double t2 = cfg.getEffect("strongman_tier2_dmg", 0.5);
        double t3 = cfg.getEffect("strongman_tier3_dmg", 0.25);
        int maxT1 = cfg.getIntEffect("strongman_tier1_max_lvl", 100);
        int maxT2 = cfg.getIntEffect("strongman_tier2_max_lvl", 200);
        double hpToDamage = cfg.getEffect("strongman_hp_to_damage_multiplier", 0.5); // 1 HP = 0.5 урона

        double bonusHP = 0;
        bonusHP += (Math.min(level, maxT1) / (double) levelsPerStep) * t1;
        if (level > maxT1) bonusHP += (Math.min(level - maxT1, maxT2 - maxT1) / (double) levelsPerStep) * t2;
        if (level > maxT2) bonusHP += ((level - maxT2) / (double) levelsPerStep) * t3;
        return bonusHP * hpToDamage;
    }

    @Override
    public String getDescription() {
        return "Усиливает удары по мере накопления опыта.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int levelsPerStep = cfg.getIntEffect("strongman_levels_per_step", 10);
        double hpToDamage = cfg.getEffect("strongman_hp_to_damage_multiplier", 0.5);
        double bonus = computeBonus(player);
        int toNext = levelsPerStep - (player.experienceLevel % levelsPerStep);

        return List.of(
                "§7Текущий уровень: §f" + player.experienceLevel,
                "§7Текущий бонус: §c+" + String.format("%.2f", bonus / hpToDamage) + " НР урона", // Показываем в HP
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double targetDamage = computeBonus(player);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;

        if (Math.abs(oldBonus - targetDamage) > 1e-6) {
            if (targetDamage > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, targetDamage, AttributeModifier.Operation.ADD_VALUE));
            } else {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }
}