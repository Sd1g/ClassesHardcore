package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
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
        ModConfig cfg = ModConfig.getInstance();
        int tpPerBonus = cfg.getIntEffect("teleporter_tp_per_bonus", 200);
        double maxBonus = cfg.getEffect("teleporter_max_bonus", 10.0);
        return "За каждые " + tpPerBonus + " успешных телепортаций макс. дистанция увеличивается на 1 блок (макс. +" + (int)maxBonus + ").";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int tpPerBonus = cfg.getIntEffect("teleporter_tp_per_bonus", 200);
        double maxBonus = cfg.getEffect("teleporter_max_bonus", 10.0);

        int count = AbilityClientState.teleporterCount;
        int bonus = (int) Math.min(count / (double)tpPerBonus, maxBonus);
        int toNext = tpPerBonus - (count % tpPerBonus);

        return List.of(
                "§7Успешных ТП: §b" + count,
                "§7Текущий бонус: §b+" + bonus + " блоков",
                "§7До след. бонуса: §f" + toNext + " ТП"
        );
    }
}