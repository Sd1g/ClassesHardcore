package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.network.ActiveTimerPayload;

import java.util.List;

public class SecondWindAbility implements Ability {
    @Override public String getId() { return "second_wind"; }
    @Override public String getDisplayName() { return "Второе дыхание"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/second_wind"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("second_wind", 900);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hp = (int) cfg.getEffect("second_wind_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("second_wind_duration_ticks", 300) / 20;
        int cd = cfg.getCooldown("second_wind", 900) / 20;
        return "Даёт " + hp + " HP поглощения на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int hp = (int) ModConfig.getInstance().getEffect("second_wind_absorption_hp", 4.0);
        return List.of("§7Поглощение: §6+" + hp + " HP");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int hp = (int) cfg.getEffect("second_wind_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("second_wind_duration_ticks", 300);

        // Просто даем поглощение равное значению из конфига
        player.setAbsorptionAmount(hp);

        ServerPlayNetworking.send(player, new ActiveTimerPayload("second_wind", dur));

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 25, 0.4, 0.8, 0.4, 0.2);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 15, 0.5, 0.5, 0.5, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.4f, 1.8f);

        player.sendOverlayMessage(Component.literal("§eВторое дыхание. §f+" + hp + " HP."));
        return true;
    }
}