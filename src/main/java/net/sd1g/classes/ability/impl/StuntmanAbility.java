package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class StuntmanAbility implements Ability {
    @Override public String getId() { return "fall_resistance"; }
    @Override public String getDisplayName() { return "Страховка"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/fall_resistance"); }

    @Override
    public String getDescription() {
        double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.3333);
        double times = mult > 0 ? (1.0 / mult) : 1.0;
        String timesText = times == Math.floor(times) ? String.valueOf((int) times) : String.format("%.1f", times);
        return "Получает в " + timesText + " раза меньше урона от падения.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.3333);
        int reduction = (int) Math.round((1.0 - mult) * 100);
        return List.of("§7Снижение урона от падения: §a" + reduction + "%");
    }
}