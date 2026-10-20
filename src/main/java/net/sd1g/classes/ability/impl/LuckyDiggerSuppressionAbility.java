package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class LuckyDiggerSuppressionAbility implements Ability {
    @Override public String getId() { return "digger_suppression"; }
    @Override public String getDisplayName() { return "Подавление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        return "Враги в радиусе получают Утомление в зависимости от дистанции.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int r1 = cfg.getIntEffect("digger_suppress_radius_1", 4);
        int r2 = cfg.getIntEffect("digger_suppress_radius_2", 8);
        int r3 = cfg.getIntEffect("digger_suppress_radius_3", 16);
        return List.of("§7Радиусы: §f" + r1 + " (III), " + r2 + " (II), " + r3 + " (I)");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int r1 = cfg.getIntEffect("digger_suppress_radius_1", 4);
        int r2 = cfg.getIntEffect("digger_suppress_radius_2", 8);
        int r3 = cfg.getIntEffect("digger_suppress_radius_3", 16);
        int amp1 = cfg.getIntEffect("digger_suppress_amp_1", 2);
        int amp2 = cfg.getIntEffect("digger_suppress_amp_2", 1);
        int amp3 = cfg.getIntEffect("digger_suppress_amp_3", 0);
        int dur = cfg.getIntEffect("digger_suppress_duration", 40);

        if (player.tickCount % 20 == 0) {
            List<ServerPlayer> targets = player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(r3));
            for (ServerPlayer target : targets) {
                if (target == player) continue;
                double dist = target.distanceTo(player);
                int amp = -1;
                if (dist <= r1) amp = amp1;
                else if (dist <= r2) amp = amp2;
                else if (dist <= r3) amp = amp3;

                if (amp >= 0) target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, dur, amp, true, false));
            }
        }
    }
}