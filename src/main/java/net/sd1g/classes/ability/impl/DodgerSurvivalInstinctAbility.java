package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class DodgerSurvivalInstinctAbility implements Ability {
    @Override public String getId() { return "dodger_survival_instinct"; }
    @Override public String getDisplayName() { return "Инстинкт выживания"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dodgesPerStep = cfg.getIntEffect("dodger_kills_per_step", 300);
        double maxBonus = cfg.getEffect("dodger_max_bonus_chance", 0.10) * 100;
        return "За каждые " + dodgesPerStep + " уклонений шанс уклонения растет на 1% (макс. +" + (int)maxBonus + "%).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dodges = AbilityClientState.dodgerDodges;
        int perStep = cfg.getIntEffect("dodger_kills_per_step", 300);
        double maxBonus = cfg.getEffect("dodger_max_bonus_chance", 0.10) * 100;

        int currentBonus = (int) Math.min((dodges / (double)perStep), maxBonus);
        int toNext = perStep - (dodges % perStep);

        return List.of(
                "§7Уклонений: §b" + dodges,
                "§7Текущий бонус: §a+" + currentBonus + "%",
                (currentBonus < maxBonus ? "§7До след. бонуса: §f" + toNext + " уклонений" : "§7Максимальный бонус")
        );
    }
}