package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;
import java.util.Random;

/**
 * Пассивка «Глубокая заморозка».
 * С шансом N% при ударе полностью обездвиживает цель на короткое время
 * (Slowness X + Mining Fatigue X).
 */
public class IceDeepFreezeAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "ice_deep_freeze"; }
    @Override public String getDisplayName() { return "Глубокая заморозка"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("ice_deep_freeze_chance", 0.05) * 100);
        int dur = cfg.getIntEffect("ice_deep_freeze_duration_ticks", 40) / 20;
        return "С шансом " + chance + "% полностью замораживает противника на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("ice_deep_freeze_chance", 0.05) * 100);
        int dur = cfg.getIntEffect("ice_deep_freeze_duration_ticks", 40);
        int amp = cfg.getIntEffect("ice_deep_freeze_amplifier", 9) + 1;
        return List.of(
                "§7Шанс: §a" + chance + "%",
                "§7Обездвиживание: §b" + toRoman(amp) + " §7на §f" + (dur / 20) + " сек."
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("ice_deep_freeze_chance", 0.05);
        if (RNG.nextDouble() >= chance) return;

        int freezeTicks = cfg.getIntEffect("ice_deep_freeze_duration_ticks", 40);
        int freezeAmp = cfg.getIntEffect("ice_deep_freeze_amplifier", 9);

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeTicks, freezeAmp, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, freezeTicks, freezeAmp, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        // Массивная вспышка заморозки
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 40, 0.8, 1.0, 0.8, 0.15);
        level.sendParticles(new DustParticleOptions(0x00C8FF, 2.0f), x, y, z, 30, 0.7, 0.9, 0.7, 0.15);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.5f), x, y, z, 20, 0.6, 0.8, 0.6, 0.1);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 15, 0.7, 0.7, 0.7, 0.15);

        level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6f, 0.6f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.4f, 0.8f);

        attacker.sendOverlayMessage(Component.literal("§bГлубокая заморозка!"));
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}