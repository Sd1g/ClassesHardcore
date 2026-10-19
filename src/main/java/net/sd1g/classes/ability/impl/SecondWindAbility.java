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

public class SecondWindAbility implements Ability {
    @Override public String getId() { return "second_wind"; }
    @Override public String getDisplayName() { return "Второе дыхание"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 900; } // 45 сек

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hp = (int) cfg.getEffect("second_wind_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("second_wind_duration_ticks", 300) / 20;
        int cdSec = cfg.getCooldown("second_wind", getBaseCooldown()) / 20;
        return "Даёт " + (hp / 2) + " золотых сердец (Поглощение) на " + dur + " сек. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int hp = (int) cfg.getEffect("second_wind_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("second_wind_duration_ticks", 300);
        int cd = cfg.getCooldown("second_wind", getBaseCooldown());
        return List.of(
                "§7Поглощение: §6+" + hp + " HP",
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int hp = (int) cfg.getEffect("second_wind_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("second_wind_duration_ticks", 300);
        // Absorption amplifier 0 = 4 HP = 2 сердца. Если hp=4 → amp 0; hp=8 → amp 1; и т.д.
        int amplifier = Math.max(0, (hp / 4) - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, amplifier, false, true, true));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 35, 0.6, 0.8, 0.6, 0.4);
        level.sendParticles(ParticleTypes.HEART, x, y, z, 12, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(new DustParticleOptions(0xFFD700, 1.8f), x, y, z, 35, 0.7, 0.8, 0.7, 0.2);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.5f), x, y, z, 25, 0.6, 0.7, 0.6, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 15, 0.5, 0.7, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.3f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.3f, 1.5f);

        player.sendOverlayMessage(Component.literal(
                "§eВторое дыхание. §f+" + (hp / 2) + " сердец на " + (dur / 20) + " сек."));
        return true;
    }
}