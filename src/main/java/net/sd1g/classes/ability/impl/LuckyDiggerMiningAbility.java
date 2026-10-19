package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class LuckyDiggerMiningAbility implements Ability {
    @Override public String getId() { return "mining_mastery"; }
    @Override public String getDisplayName() { return "Мастерство шахтёра"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/mining_mastery"); }

    @Override
    public String getDescription() {
        return "За каждые 200 добытых руд усиливает бонус добычи на 1 ступень (макс. 4).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        String mode = AbilityClientState.luckyDiggerMode;
        int total = AbilityClientState.luckyDiggerTotalOre;
        int level = AbilityClientState.luckyDiggerLevel;
        int toNext = 200 - (total % 200);
        if (total % 200 == 0 && total > 0) toNext = 200;
        String modeName = "efficiency".equals(mode) ? "§bЭффективность" : "§aУдача";

        return List.of(
                "§7Добыто руды: §f" + total,
                "§7Уровень бонуса: §f" + level + (level >= 4 ? " §7(макс.)" : ""),
                "§7Активный бонус: " + modeName,
                "§7До следующего уровня: §f" + toNext + " блоков"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);

        if (data.luckyDiggerLevel <= 0) {
            passiveAura(player, data);
            return;
        }

        if ("efficiency".equals(data.luckyDiggerMode)) {
            int amplifier = Math.min(data.luckyDiggerLevel - 1, 4);
            MobEffectInstance cur = player.getEffect(MobEffects.HASTE);
            if (cur == null || cur.getAmplifier() != amplifier || cur.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
            }
        }

        passiveAura(player, data);
    }

    private static void passiveAura(ServerPlayer player, PlayerClassData data) {
        if (player.tickCount % 12 != 0) return;
        boolean luckMode = "luck".equals(data.luckyDiggerMode);
        int color = luckMode ? 0x33FF4D : 0x3399FF;

        ServerLevel level = player.level();
        level.sendParticles(new DustParticleOptions(color, 0.9f), player.getX(), player.getY() + 0.05, player.getZ(), 3, 0.4, 0.03, 0.4, 0.02);
    }

    public static void applyCurrentEffect(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.LUCK);
        if (data.luckyDiggerLevel <= 0) return;

        if ("efficiency".equals(data.luckyDiggerMode)) {
            int amplifier = Math.min(data.luckyDiggerLevel - 1, 4);
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
        } else if ("luck".equals(data.luckyDiggerMode)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, Math.min(data.luckyDiggerLevel - 1, 4), true, false));
        }
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.LUCK);
    }
}