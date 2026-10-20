package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class ArcherHunterMasteryAbility implements Ability {
    @Override public String getId() { return "archer_hunter_mastery"; }
    @Override public String getDisplayName() { return "Мастерство охотника"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int perStep = cfg.getIntEffect("archer_hunter_kills_per_step", 100);
        int maxRed = cfg.getIntEffect("archer_hunter_max_reduction_ticks", 200);
        return "За каждые " + perStep + " убийств из лука КД отпрыгивания сокращается на 1 сек (макс. " + (maxRed/20) + " сек).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int kills = AbilityClientState.archerBowKills;
        int perStep = cfg.getIntEffect("archer_hunter_kills_per_step", 100);
        int maxRed = cfg.getIntEffect("archer_hunter_max_reduction_ticks", 200);

        int reduction = Math.min((kills / perStep), maxRed / 20);
        int toNext = perStep - (kills % perStep);

        return List.of(
                "§7Убийств из лука: §b" + kills,
                "§7Сокращение КД: §a-" + reduction + " сек.",
                "§7До след. сокращения: §f" + toNext + " убийств"
        );
    }
}