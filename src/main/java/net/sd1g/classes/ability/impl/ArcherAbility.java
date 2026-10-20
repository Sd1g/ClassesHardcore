package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ArcherAbility implements Ability {
    @Override public String getId() { return "archer_mastery"; }
    @Override public String getDisplayName() { return "Мастерство лучника"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double mult = cfg.getEffect("archer_draw_speed_multiplier", 1.5);
        double dmg = cfg.getEffect("archer_pure_damage", 4.0) / 2.0;
        return "Лук натягивается в " + mult + " раза быстрее. Полный выстрел наносит +" + dmg + " НР чистого урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Кулдаун чистого урона: 1.5 сек.");
    }
}