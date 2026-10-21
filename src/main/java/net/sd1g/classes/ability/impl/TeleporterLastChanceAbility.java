package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class TeleporterLastChanceAbility implements Ability {
    @Override public String getId() { return "last_chance"; }
    @Override public String getDisplayName() { return "Последний шанс"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/last_chance"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("last_chance_chance", 0.30) * 100);
        int radius = (int) cfg.getEffect("last_chance_radius", 30.0);
        return "При срабатывании Тотема бессмертия с шансом " + chance + "% переносит в случайную безопасную точку в радиусе " + radius + " блоков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Логика срабатывания обрабатывается в фоне.");
    }
}