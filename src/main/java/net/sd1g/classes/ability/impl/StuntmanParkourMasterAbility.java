package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class StuntmanParkourMasterAbility implements Ability {
    @Override public String getId() { return "parkour_master"; }
    @Override public String getDisplayName() { return "Мастер паркура"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/parkour_master"); }

    @Override
    public String getDescription() {
        return "За каждые 500 блоков лазания длительность Прыгучести V увеличивается на 1 сек, а её перезарядка сокращается на 1 сек (макс +5 сек / -5 сек). КД переключения лазания растет до 10 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int blocks = AbilityClientState.stuntmanClimbedBlocks; // Берем из клиентского кэша
        int bonusSec = Math.min(blocks / 500, 5);
        int toNext = 500 - (blocks % 500);

        return List.of(
                "§7Блоков пройдено: §b" + blocks,
                "§7Текущий бонус: §a+" + bonusSec + " сек.",
                "§7До след. бонуса: §f" + toNext + " бл."
        );
    }
}