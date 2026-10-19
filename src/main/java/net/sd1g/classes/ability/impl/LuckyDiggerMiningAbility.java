package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class LuckyDiggerMiningAbility implements Ability {
    @Override public String getId() { return "mining_mastery"; }
    @Override public String getDisplayName() { return "Мастерство шахтёра"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int perLevel = cfg.getIntEffect("mining_mastery_blocks_per_level", 200);
        int maxLevel = cfg.getIntEffect("mining_mastery_max_level", 4);
        return "За каждые " + perLevel + " добытых руд даёт +1 к уровню бонуса (макс. " + maxLevel + "). " +
                "Враги в радиусе 20 блоков получают Утомление.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        String mode = AbilityClientState.luckyDiggerMode;
        int total = AbilityClientState.luckyDiggerTotalOre;
        int level = AbilityClientState.luckyDiggerLevel;
        int perLevel = ModConfig.getInstance().getIntEffect("mining_mastery_blocks_per_level", 200);
        int maxLevel = ModConfig.getInstance().getIntEffect("mining_mastery_max_level", 4);
        int toNext = perLevel > 0 ? perLevel - (total % perLevel) : 0;
        if (total % perLevel == 0 && total > 0) toNext = perLevel;
        String modeName = "efficiency".equals(mode) ? "§bЭффективность" : "§aУдача";
        String levelText = level + (level >= maxLevel ? " §7(макс.)" : "");
        return List.of(
                "§7Добыто руды: §f" + total,
                "§7Уровень бонуса: §f" + levelText,
                "§7Активный бонус: " + modeName,
                "§7До следующего уровня: §f" + toNext + " блоков"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);

        // Утомление для врагов (радиус 20)
        if (player.tickCount % 20 == 0) {
            ModConfig cfg = ModConfig.getInstance();
            double radius = cfg.getEffect("digger_exhaust_radius", 20.0);
            int maxAmp = cfg.getIntEffect("digger_exhaust_max_amp", 2);
            int minAmp = cfg.getIntEffect("digger_exhaust_min_amp", 0);
            int midAmp = (maxAmp + minAmp) / 2;

            List<ServerPlayer> targets = player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius));
            for (ServerPlayer target : targets) {
                if (target == player) continue;
                double dist = target.distanceTo(player);
                int amp = maxAmp;
                if (dist > 15) amp = minAmp;
                else if (dist > 5) amp = midAmp;

                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, amp, true, false));
            }
        }

        if (data.luckyDiggerLevel <= 0) {
            passiveAura(player, data);
            return;
        }

        if ("efficiency".equals(data.luckyDiggerMode)) {
            int maxAmp = ModConfig.getInstance().getIntEffect("mining_mastery_effect_max_amplifier", 4);
            int amplifier = Math.min(data.luckyDiggerLevel - 1, maxAmp);
            MobEffectInstance cur = player.getEffect(MobEffects.HASTE);
            boolean wasAbsent = cur == null;
            boolean wrongAmp = cur != null && cur.getAmplifier() != amplifier;
            boolean lowDur = cur != null && cur.getDuration() < 40;
            if (wasAbsent || wrongAmp || lowDur) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
            }
            if (wasAbsent && data.luckyDiggerLevel > 0) {
                ServerLevel level = player.level();
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();
                level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);
                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 12, 0.5, 0.5, 0.5, 0.3);
            }
        }

        passiveAura(player, data);
    }

    private static void passiveAura(ServerPlayer player, PlayerClassData data) {
        if (player.tickCount % 12 != 0) return;
        boolean luckMode = "luck".equals(data.luckyDiggerMode);
        int color = luckMode ? 0x33FF4D : 0x3399FF;

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        level.sendParticles(new DustParticleOptions(color, 0.9f), x, y + 0.05, z, 3, 0.4, 0.03, 0.4, 0.02);
        if (player.tickCount % 24 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.4 + i * 0.6;
                level.sendParticles(new DustParticleOptions(color, 0.7f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5, y + dy, z + (player.getRandom().nextDouble() - 0.5) * 0.5, 1, 0, 0.02, 0, 0);
            }
        }
        if (player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.CRIT, x, y + 0.6, z, 2, 0.4, 0.4, 0.4, 0.05);
        }
    }

    public static void applyCurrentEffect(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.LUCK);
        if (data.luckyDiggerLevel <= 0) return;

        if ("efficiency".equals(data.luckyDiggerMode)) {
            int maxAmp = ModConfig.getInstance().getIntEffect("mining_mastery_effect_max_amplifier", 4);
            int amplifier = Math.min(data.luckyDiggerLevel - 1, maxAmp);
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
        }
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.LUCK);
    }
}