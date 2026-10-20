package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class TitanAbility implements Ability {
    @Override public String getId() { return "titan_passive"; }
    @Override public String getDisplayName() { return "Каменная кожа"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 0) + 1;
        return "Постоянно дает Сопротивление " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Пассивный эффект");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 0);
        MobEffectInstance current = player.getEffect(MobEffects.RESISTANCE);
        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, amp, true, false));
        }
    }
}