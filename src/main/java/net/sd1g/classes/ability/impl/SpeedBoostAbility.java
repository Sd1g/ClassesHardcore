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

import java.util.List;

public class SpeedBoostAbility implements Ability {
    @Override public String getId() { return "speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("speed_boost_amplifier", 0);
        return "Постоянно даёт Скорость " + toRoman(amp + 1) + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = ModConfig.getInstance().getIntEffect("speed_boost_amplifier", 0);
        return List.of("§7Эффект: §bСкорость " + toRoman(amp + 1) + " §7(постоянно)");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amplifier = ModConfig.getInstance().getIntEffect("speed_boost_amplifier", 0);

        MobEffectInstance current = player.getEffect(MobEffects.SPEED);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != amplifier;
        boolean lowDuration = current != null && current.getDuration() < 40;
        boolean needsRefresh = wasAbsent || wrongAmp || lowDuration;

        if (needsRefresh) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amplifier, true, false));
        }
        // Звук — только при первой выдаче
        if (wasAbsent) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.6f, 1.6f);
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Золотые искры у ног — плотные, раз в 8 тиков
        if (player.tickCount % 8 == 0) {
            level.sendParticles(new DustParticleOptions(0xFFAA00, 1.0f),
                    x, y + 0.05, z, 4, 0.4, 0.03, 0.4, 0.02);
        }
        // Столб тёплого золота — восходит, раз в 12 тиков
        if (player.tickCount % 12 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0xFFD580, 0.8f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        // Редкий SWEEP_ATTACK — полоска скорости
        if (player.tickCount % 25 == 0) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.6, z, 1, 0.3, 0.1, 0.3, 0.0);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.SPEED);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}