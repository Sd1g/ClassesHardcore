package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.config.ModConfig;

public class WarriorSpeedBoostAbility implements Ability {
    @Override public String getId() { return "warrior_speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return true; }
    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/speed_boost"); }

    @Override
    public TooltipData buildTooltip(Player player) {
        int amp = ModConfig.getInstance().getInt("warrior", "warrior_speed_boost.amplifier", 0) + 1;
        return TooltipData.builder("Постоянно дает эффект Скорости.")
                .currentBonus("Скорость " + amp)
                .build();
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amp = ModConfig.getInstance().getInt("warrior", "warrior_speed_boost.amplifier", 0);
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);

        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
        }
    }
}