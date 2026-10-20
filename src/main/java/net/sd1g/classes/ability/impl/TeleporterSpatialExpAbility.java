package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class TeleporterSpatialExpAbility implements Ability {
    @Override public String getId() { return "spatial_exp"; }
    @Override public String getDisplayName() { return "Пространственный опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/spatial_exp"); }

    @Override
    public String getDescription() {
        return "За каждые 200 успешных телепортаций макс. дистанция увеличивается на 1 блок (макс. +10). Телепортации от тотема не считаются.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int count = AbilityClientState.teleporterCount;
        int bonus = Math.min(count / 200, 10);
        int toNext = 200 - (count % 200);

        return List.of(
                "§7Успешных ТП: §b" + count,
                "§7Текущий бонус: §b+" + bonus + " блоков",
                "§7До след. бонуса: §f" + toNext + " ТП"
        );
    }
}