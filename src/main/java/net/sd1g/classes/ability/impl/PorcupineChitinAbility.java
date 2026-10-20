package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class PorcupineChitinAbility implements Ability {
    @Override public String getId() { return "porcupine_chitin"; }
    @Override public String getDisplayName() { return "Хитиновый покров"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_chitin"); }

    @Override
    public String getDescription() {
        return "Постоянно получает на 20% меньше входящего урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Снижение урона: §a20%");
    }
}