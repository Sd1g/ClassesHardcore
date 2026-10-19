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

public class TitanAbility implements Ability {
    @Override public String getId() { return "titan_resistance"; }
    @Override public String getDisplayName() { return "Каменная кожа"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 1);
        return "Постоянно даёт Сопротивление " + toRoman(amp + 1) + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 1);
        return List.of("§7Эффект: §bСопротивление " + toRoman(amp + 1) + " §7(постоянно)");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int amplifier = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 1);

        MobEffectInstance current = player.getEffect(MobEffects.RESISTANCE);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != amplifier;
        boolean lowDuration = current != null && current.getDuration() < 40;
        boolean needsRefresh = wasAbsent || wrongAmp || lowDuration;

        if (needsRefresh) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, amplifier, true, false));
        }
        // Звук — только при первой выдаче, а не при каждом refresh
        if (wasAbsent) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0f, 0.7f);
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Пыль под ногами — плотная, раз в 10 тиков
        if (player.tickCount % 10 == 0) {
            level.sendParticles(new DustParticleOptions(0x7F7F7F, 1.0f),
                    x, y + 0.1, z, 4, 0.5, 0.05, 0.5, 0.02);
        }
        // Столб из серых частиц — восходит вверх, раз в 15 тиков
        if (player.tickCount % 15 == 0) {
            for (int i = 0; i < 3; i++) {
                double dy = 0.4 + i * 0.5;
                level.sendParticles(new DustParticleOptions(0x8A8A8A, 0.9f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.6,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.6,
                        1, 0, 0.02, 0, 0);
            }
        }
        // Бронзовое кольцо у ног раз в 30 тиков
        if (player.tickCount % 30 == 0) {
            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                double rx = x + Math.cos(angle) * 0.9;
                double rz = z + Math.sin(angle) * 0.9;
                level.sendParticles(new DustParticleOptions(0x8B7355, 1.0f),
                        rx, y + 0.05, rz, 1, 0, 0, 0, 0);
            }
        }
        // Редкие искры CRIT — «трескается камень»
        if (player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.CRIT, x, y + 0.8, z, 3, 0.5, 0.4, 0.5, 0.05);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.RESISTANCE);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}