package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class HeroVillageDefenderAbility implements Ability {
    @Override public String getId() { return "hero_village_defender"; }
    @Override public String getDisplayName() { return "Защитник деревни"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    // ИКОНКА УЖЕ БЫЛА, НО ПРОВЕРЯЕМ ПУТЬ
    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/hero_village_defender"); }

    @Override
    public String getDescription() {
        return "Наносит на 200% больше урона мобам, участвующим в рейдах. На игроков бонус не действует.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус: §c+200% урона §7по рейдерам");
    }
}