package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class CombatVeteranAbility implements Ability {
    @Override public String getId() { return "combat_veteran"; }
    @Override public String getDisplayName() { return "Боевой ветеран"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_veteran"); }

    @Override
    public String getDescription() {
        return "За каждые 2000 убийств мобов мечом эффективность способности «Исцеление» увеличивается на 0.5 HP. Максимальный бонус: +10 HP.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int kills = AbilityClientState.warriorSwordKills; // Берем из клиентского кэша
        float bonusHP = Math.min((kills / 2000) * 0.5f, 10.0f);
        int toNext = 2000 - (kills % 2000);
        if (kills >= 20000) toNext = 0;

        return List.of(
                "§7Убийств мечом: §b" + kills,
                "§7Текущий бонус: §a+" + bonusHP + " HP",
                (toNext > 0 ? "§7До след. бонуса: §f" + toNext + " убийств" : "§7Максимальный бонус")
        );
    }
}