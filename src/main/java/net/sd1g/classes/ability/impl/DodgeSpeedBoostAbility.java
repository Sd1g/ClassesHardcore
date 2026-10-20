package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class DodgeSpeedBoostAbility implements Ability {
    @Override public String getId() { return "dodge_speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("dodge_speed_boost", 500);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1) + 1;
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200) / 20;
        int cd = getBaseCooldown() / 20;
        return "Скорость " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Быстрый откат");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1);
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, amp, false, true));
        return true;
    }
}