package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class CombatExperienceAbility implements Ability {
    @Override public String getId() { return "combat_experience"; }
    @Override public String getDisplayName() { return "Боевой опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_experience"); }

    @Override
    public String getDescription() {
        return "Наносит мечом на 15% больше итогового физического урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус: §c+15% урона §7(только мечом)");
    }
    // Логика удалена, так как она уже есть в WarriorSwordDamageMixin
}