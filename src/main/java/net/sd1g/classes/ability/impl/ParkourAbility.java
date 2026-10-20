package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class ParkourAbility implements Ability {
    @Override public String getId() { return "parkour"; }
    @Override public String getDisplayName() { return "Паркур"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("parkour", 600);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("parkour_jump_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("parkour_duration_ticks", 220) / 20;
        int cd = cfg.getCooldown("parkour", 600) / 20;
        return "Прыгучесть " + amp + " на " + dur + " сек. Базовая перезарядка: " + cd + " сек. (уменьшается от прогрессии)";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7КД: §f30 сек. (уменьшается от прогрессии)");
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int blocksPerStep = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);
        int reduction = cfg.getIntEffect("stuntman_master_cooldown_reduction", 20);

        int bonusTicks = Math.min((data.stuntmanClimbedBlocks / blocksPerStep) * reduction, maxSteps * reduction);
        return getBaseCooldown() - bonusTicks;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int blocksPerStep = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);
        int bonusTicks = cfg.getIntEffect("stuntman_master_duration_bonus", 20);

        int extraDuration = Math.min((data.stuntmanClimbedBlocks / blocksPerStep) * bonusTicks, maxSteps * bonusTicks);
        int duration = cfg.getIntEffect("parkour_duration_ticks", 220) + extraDuration;
        int amp = cfg.getIntEffect("parkour_jump_amplifier", 4);

        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, amp, false, true, true));
        player.sendOverlayMessage(Component.literal("§eПаркур на " + (duration / 20) + " сек."));
        return true;
    }
}