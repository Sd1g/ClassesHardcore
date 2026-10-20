package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class StuntmanParkourMasterAbility implements Ability {
    @Override public String getId() { return "parkour_master"; }
    @Override public String getDisplayName() { return "Мастер паркура"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int blocks = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        return "За каждые " + blocks + " блоков лазания улучшает Паркур и Сцепление.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int blocks = AbilityClientState.stuntmanClimbedBlocks;
        int perStep = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);

        int steps = Math.min(blocks / perStep, maxSteps);
        int toNext = perStep - (blocks % perStep);

        return List.of(
                "§7Блоков пройдено: §b" + blocks,
                "§7Текущий бонус: §a+" + steps + " сек.",
                "§7До след. бонуса: §f" + toNext + " бл."
        );
    }
}