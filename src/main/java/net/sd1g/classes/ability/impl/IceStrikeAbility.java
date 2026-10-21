package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class IceStrikeAbility implements Ability {
    @Override public String getId() { return "ice_strike"; }
    @Override public String getDisplayName() { return "Ледяной удар"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_strike"); }

    static int computeSlownessAmplifier(Player player) {
        if (player == null) return 0;
        ModConfig cfg = ModConfig.getInstance();
        int levelsPerStep = cfg.getIntEffect("ice_strike_levels_per_amplifier_step", 50);
        int maxAmp = cfg.getIntEffect("ice_strike_max_amplifier", 2);
        int bonus = levelsPerStep > 0 ? player.experienceLevel / levelsPerStep : 0;
        return Math.min(bonus, maxAmp);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double dmg = cfg.getEffect("ice_strike_bonus_damage", 1.5);
        int dur = cfg.getIntEffect("ice_strike_slowness_duration_ticks", 100) / 20;
        return "Удар добавляет " + (dmg / 2.0) + " НР чистого урона и Замедление на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = computeSlownessAmplifier(player);
        return List.of(
                "§7Чистый урон: §c+" + (ModConfig.getInstance().getEffect("ice_strike_bonus_damage", 1.5) / 2.0) + " НР",
                "§7Замедление: §b" + toRoman(amp + 1)
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();

        int duration = cfg.getIntEffect("ice_strike_slowness_duration_ticks", 100);
        int amplifier = computeSlownessAmplifier(attacker);

        // 1. Наложение замедления (срабатывает всегда при ударе)
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() / 2.0);
        double z = target.getZ();

        // Эффект инея
        level.sendParticles(new DustParticleOptions(0x00FFFF, 1.5f), x, y, z, 15, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 10, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, x, y, z, SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);

        // 2. Чистый урон (раз в секунду)
        if (!data.isOnCooldown("ice_strike_cd", now)) {
            float damage = (float) cfg.getEffect("ice_strike_bonus_damage", 1.5);
            PureDamage.apply(attacker, target, damage);
            data.setCooldown("ice_strike_cd", now, 20);

            // Эффект разбивания льда
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 20, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 15, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.6f, 1.8f);
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}