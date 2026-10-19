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

public class MonolithAbility implements Ability {
    @Override public String getId() { return "monolith"; }
    @Override public String getDisplayName() { return "Монолит"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1200; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100) / 20;
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3) + 1;
        int cdSec = cfg.getCooldown("monolith", getBaseCooldown()) / 20;
        return "Даёт Сопротивление " + toRoman(amp) + " на " + dur + " сек. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3) + 1;
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100);
        int cd = cfg.getCooldown("monolith", getBaseCooldown());
        return List.of(
                "§7Эффект: §bСопротивление " + toRoman(amp),
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100);
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur, amp, false, true, true));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 30, 0.7, 1.0, 0.7, 0.2);
        level.sendParticles(new DustParticleOptions(0x7F7F7F, 1.8f), x, y, z, 40, 0.8, 1.0, 0.8, 0.15);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 20, 0.6, 0.9, 0.6, 0.15);

        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.3f, 0.6f);
        level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.sendOverlayMessage(Component.literal("§7Монолит. §fСопротивление " + toRoman(amp + 1) + "."));
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