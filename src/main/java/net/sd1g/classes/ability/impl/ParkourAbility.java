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
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ParkourAbility implements Ability {
    @Override public String getId() { return "parkour"; }
    @Override public String getDisplayName() { return "Паркур"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("parkour_jump_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("parkour_duration_ticks", 220) / 20;
        int cdSec = cfg.getCooldown("parkour", getBaseCooldown()) / 20;
        return "Даёт Прыгучесть " + toRoman(amp) + " на " + dur + " сек. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("parkour_jump_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("parkour_duration_ticks", 220);
        int cd = cfg.getCooldown("parkour", getBaseCooldown());
        return List.of(
                "§7Эффект: §bПрыгучесть " + toRoman(amp),
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("parkour_jump_amplifier", 4);
        int dur = cfg.getIntEffect("parkour_duration_ticks", 220);
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, dur, amp, false, true, true));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Золотое кольцо
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.1;
            double rz = z + Math.sin(angle) * 1.1;
            level.sendParticles(new DustParticleOptions(0xFFD700, 1.5f),
                    rx, y + 0.05, rz, 1, 0, 0.05, 0, 0);
        }
        // Подкидывающие частицы
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.3, z, 20, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.8, z, 15, 0.5, 0.3, 0.5, 0.1);
        level.sendParticles(new DustParticleOptions(0xFFD700, 1.8f), x, y + 0.8, z, 20, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.3f), x, y + 1.2, z, 12, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.3f, 1.4f);
        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 1.3f, 1.6f);

        player.sendOverlayMessage(Component.literal("§eПаркур. §fПрыгучесть " + toRoman(amp + 1) + "."));
        return true;
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}