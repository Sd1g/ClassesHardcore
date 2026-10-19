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

public class DodgeSpeedBoostAbility implements Ability {
    @Override public String getId() { return "dodge_speed_boost"; }
    @Override public String getDisplayName() { return "Ускорение"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 500; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200) / 20;
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1) + 1;
        int cdSec = cfg.getCooldown("dodge_speed_boost", getBaseCooldown()) / 20;
        return "Даёт Скорость " + toRoman(amp) + " на " + dur + " сек. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200);
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1) + 1;
        int cd = cfg.getCooldown("dodge_speed_boost", getBaseCooldown());
        return List.of(
                "§7Эффект: §bСкорость " + toRoman(amp),
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("dodge_speed_duration_ticks", 200);
        int amp = cfg.getIntEffect("dodge_speed_amplifier", 1);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, amp, false, true, true));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // ---- Активация: циановый вихрь вокруг ног ----
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.1;
            double rz = z + Math.sin(angle) * 1.1;
            level.sendParticles(new DustParticleOptions(0x4DFFFF, 1.3f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.3, z, 6, 0.6, 0.1, 0.6, 0.0);
        level.sendParticles(new DustParticleOptions(0x4DFFFF, 1.0f), x, y + 0.5, z, 15, 0.5, 0.4, 0.5, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.8, z, 10, 0.4, 0.4, 0.4, 0.05);

        // Активка — 2 звука: ускорение + короткий рывок
        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 1.2f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.8f);

        player.sendOverlayMessage(Component.literal("§bУскорение. §fСкорость " + toRoman(amp + 1) + "."));
        return true;
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}