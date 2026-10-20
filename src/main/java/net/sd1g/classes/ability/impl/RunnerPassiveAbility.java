package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class RunnerPassiveAbility implements Ability {
    @Override public String getId() { return "runner_passive"; }
    @Override public String getDisplayName() { return "Разгон"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1) + 1;
        return "Постоянно даёт Скорость " + amp + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Пассивный эффект");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) return;

        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        MobEffectInstance current = player.getEffect(MobEffects.SPEED);

        if (current == null || current.getAmplifier() != amp || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
        }
    }

    public static void applyPassiveSpeed(ServerPlayer player) {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amp, true, false));
    }
}