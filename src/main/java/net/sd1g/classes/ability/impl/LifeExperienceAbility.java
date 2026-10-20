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
        double hp = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);
        return "За каждые " + per + " уровней опыта +" + hp + " НР к максимуму.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int per = cfg.getIntEffect("life_exp_levels_per_point", 5);
        double hpPerPoint = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);

        int level = player.experienceLevel;
        int points = per > 0 ? level / per : 0;
        double bonus = points * hpPerPoint;
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
        double hpPerPoint = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);

        int points = per > 0 ? player.experienceLevel / per : 0;
        double targetBonus = points * hpPerPoint;

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;

        if (Math.abs(oldBonus - targetBonus) > 1e-6) {
            if (targetBonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, targetBonus, AttributeModifier.Operation.ADD_VALUE));
            } else if (existing != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}