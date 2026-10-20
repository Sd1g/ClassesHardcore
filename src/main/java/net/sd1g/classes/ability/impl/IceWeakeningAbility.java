package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class IceWeakeningAbility implements Ability {
    @Override public String getId() { return "ice_weakening"; }
    @Override public String getDisplayName() { return "Ледяное ослабление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double reduction = cfg.getEffect("ice_weakening_damage_per_level", 0.5);
        return "Каждый уровень эффекта «Замедление» на противнике снижает его исходящий урон на " + reduction + " НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Логика обрабатывается через Mixin.");
    }
}