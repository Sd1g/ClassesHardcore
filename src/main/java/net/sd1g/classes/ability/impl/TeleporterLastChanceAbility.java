package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class TeleporterLastChanceAbility implements Ability {
    @Override public String getId() { return "last_chance"; }
    @Override public String getDisplayName() { return "Последний шанс"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("last_chance_chance", 0.20) * 100);
        int radius = (int) cfg.getEffect("last_chance_radius", 10.0);
        return "При срабатывании Тотема бессмертия с шансом " + chance
                + "% телепортирует в случайное место в радиусе " + radius + " блоков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("last_chance_chance", 0.20) * 100);
        int radius = (int) cfg.getEffect("last_chance_radius", 10.0);
        return List.of(
                "§7Шанс срабатывания: §a" + chance + "%",
                "§7Радиус телепорта: §f" + radius + " блоков"
        );
    }
}