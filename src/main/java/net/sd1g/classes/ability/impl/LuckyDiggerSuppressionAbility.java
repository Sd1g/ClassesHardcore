package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class LuckyDiggerSuppressionAbility implements Ability {
    @Override public String getId() { return "digger_suppression"; }
    @Override public String getDisplayName() { return "Подавление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/digger_suppression"); }

    @Override
    public String getDescription() {
        return "Враги в радиусе 4 блоков получают Утомление III, в радиусе 8 — II, в радиусе 16 — I.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Радиус: §f4-16 блоков");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            List<ServerPlayer> targets = player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(16.0));
            for (ServerPlayer target : targets) {
                if (target == player) continue;
                double dist = target.distanceTo(player);
                int amp = -1;
                if (dist <= 4) amp = 2;      // Утомление III
                else if (dist <= 8) amp = 1; // Утомление II
                else if (dist <= 16) amp = 0; // Утомление I

                if (amp >= 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, amp, true, false));
                }
            }
        }
    }
}