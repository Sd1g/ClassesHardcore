package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.config.ModConfig;

public class WarriorCombatVeteranAbility implements Ability {
    @Override public String getId() { return "warrior_combat_veteran"; }
    @Override public String getDisplayName() { return "Боевой ветеран"; }
    @Override public boolean isPassive() { return true; }
    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_veteran"); }

    @Override
    public TooltipData buildTooltip(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double perLevel = cfg.getInt("warrior", "warrior_veteran.kills_per_step", 2000);
        double maxBonusHp = cfg.get("warrior", "warrior_veteran.max_hp_bonus", 10.0);
        double hpPerStep = cfg.get("warrior", "warrior_veteran.hp_per_step", 0.5);
        double timePerStep = cfg.get("warrior", "warrior_veteran.cast_time_per_step_ticks", 4.0) / 20.0;
        double maxTimeBonus = cfg.get("warrior", "warrior_veteran.max_cast_time_bonus_ticks", 80.0) / 20.0;

        return TooltipData.builder("Увеличивает эффективность «Исцеления» за убийства мечом.")
                .perLevel("+" + hpPerStep + " HP, +" + String.format("%.1f", timePerStep) + "с каста")
                .required((int)perLevel + " убийств")
                .extra("§7Максимальный бонус: §a+" + maxBonusHp + " HP, +" + String.format("%.1f", maxTimeBonus) + "с каста")
                .build();
    }
}