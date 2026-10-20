package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class FarmerAbility implements Ability {
    @Override public String getId() { return "farmer_passive"; }
    @Override public String getDisplayName() { return "Мастерство мотыги"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hoe = (int) Math.round((cfg.getEffect("farmer_hoe_damage_multiplier", 2.0) - 1.0) * 100);
        int other = (int) Math.round((1.0 - cfg.getEffect("farmer_other_damage_multiplier", 0.2)) * 100);
        return "Урон мотыгой +" + hoe + "%. Другое оружие -" + other + "%. Растет от убийств.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int mobsPerStep = cfg.getIntEffect("farmer_mobs_per_bonus_point", 100);
        double bonusPerMobStep = cfg.getEffect("farmer_bonus_per_mob_step", 0.5) / 2.0; // в HP
        double bonusPerPlayer = cfg.getEffect("farmer_bonus_per_player_kill", 10.0) / 2.0; // в HP

        int mobs = AbilityClientState.farmerMobKills;
        int players = AbilityClientState.farmerPlayerKills;

        int step = mobsPerStep > 0 ? mobs / mobsPerStep : 0;
        double accumulatedBonus = (step * bonusPerMobStep) + (players * bonusPerPlayer);
        int toNext = mobsPerStep - (mobs % mobsPerStep);

        return List.of(
                "§7Убийств мобов: §b" + mobs,
                "§7Убийств игроков: §c" + players,
                "§7Бонус за " + mobsPerStep + " мобов: §a+" + bonusPerMobStep + " HP",
                "§7Бонус за игрока: §a+" + bonusPerPlayer + " HP",
                "§7Текущий бонус: §c+" + accumulatedBonus + " HP",
                "§7До след. шага: §f" + toNext + " мобов"
        );
    }
}