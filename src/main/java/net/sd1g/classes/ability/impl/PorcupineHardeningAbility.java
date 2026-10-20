package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class PorcupineHardeningAbility implements Ability {
    @Override public String getId() { return "porcupine_hardening"; }
    @Override public String getDisplayName() { return "Закалка шипов"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hits = cfg.getIntEffect("porcupine_hardening_hits_per_step", 500);
        return "За каждые " + hits + " полученных ударов радиус «Шипов» растет.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int hits = AbilityClientState.porcupineHitsTaken;
        int perStep = cfg.getIntEffect("porcupine_hardening_hits_per_step", 500);
        double radPerStep = cfg.getEffect("porcupine_hardening_radius_per_step", 0.5);
        double maxSteps = cfg.getEffect("porcupine_hardening_max_steps", 4.0);

        double bonus = Math.min((hits / (double)perStep) * radPerStep, maxSteps * radPerStep);
        int toNext = perStep - (hits % perStep);

        return List.of(
                "§7Получено ударов: §b" + hits,
                "§7Текущий бонус: §a+" + bonus + " блоков",
                "§7До след. бонуса: §f" + toNext + " ударов"
        );
    }
}