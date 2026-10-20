package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class BruteAbility implements Ability {
    @Override public String getId() { return "brute_passive"; }
    @Override public String getDisplayName() { return "Мастер булавы"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int mace = (int) Math.round((cfg.getEffect("brute_mace_damage_multiplier", 1.5) - 1.0) * 100);
        int other = (int) Math.round((1.0 - cfg.getEffect("brute_other_damage_multiplier", 0.3)) * 100);
        return "Урон булавой +" + mace + "%. Другое оружие наносит на " + other + "% меньше.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Множители урона из balance.json");
    }
}