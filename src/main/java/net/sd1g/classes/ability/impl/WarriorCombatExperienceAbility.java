package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.config.ModConfig;

public class WarriorCombatExperienceAbility implements Ability {
    @Override public String getId() { return "warrior_combat_experience"; }
    @Override public String getDisplayName() { return "Боевой опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_experience"); }

    @Override
    public TooltipData buildTooltip(Player player) {
        double percent = ModConfig.getInstance().get("warrior", "warrior_combat_experience.sword_bonus_percent", 15.0);
        return TooltipData.builder("Наносит мечом на " + (int)percent + "% больше итогового физического урона.")
                .build();
    }
}