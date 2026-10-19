package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class BruteAbility implements Ability {
    @Override public String getId() { return "mace_mastery"; }
    @Override public String getDisplayName() { return "Мастер булавы"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int maceBonus = (int) Math.round((cfg.getEffect("brute_mace_damage_multiplier", 1.5) - 1.0) * 100);
        int otherPenalty = (int) Math.round((1.0 - cfg.getEffect("brute_other_damage_multiplier", 0.3)) * 100);
        return "Урон булавы +" + maceBonus + "%, но любое другое оружие наносит на "
                + otherPenalty + "% меньше урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double maceBonus = cfg.getEffect("brute_mace_damage_multiplier", 1.5);
        double otherPenalty = cfg.getEffect("brute_other_damage_multiplier", 0.3);
        int maceBonusPercent = (int) Math.round((maceBonus - 1.0) * 100);
        int otherPenaltyPercent = (int) Math.round((1.0 - otherPenalty) * 100);

        return List.of(
                "§7Булава: §a+" + maceBonusPercent + "% §7урона",
                "§7Другое оружие: §c−" + otherPenaltyPercent + "% §7урона"
        );
    }
}