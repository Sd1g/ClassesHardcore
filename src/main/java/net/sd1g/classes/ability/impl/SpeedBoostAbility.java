package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class SpeedBoostAbility implements Ability {
    @Override public String getId() { return "speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0) + 1;
        return "Постоянно даёт Скорость " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0) + 1;
        return List.of("§7Эффект: §bСкорость " + amp);
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amp = ModConfig.getInstance().getIntEffect("warrior_speed_amp", 0);
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);
        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
        }
    }
}