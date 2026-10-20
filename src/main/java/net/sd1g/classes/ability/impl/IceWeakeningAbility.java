package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class IceWeakeningAbility implements Ability {
    @Override public String getId() { return "ice_weakening"; }
    @Override public String getDisplayName() { return "Ледяное ослабление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_weakening"); }

    @Override
    public String getDescription() {
        return "Каждый уровень эффекта «Замедление» на противнике снижает его исходящий урон на 0,5 НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of(
                "§7Ослабление: §c−0.5 НР §7урона за уровень Замедления"
        );
    }
}