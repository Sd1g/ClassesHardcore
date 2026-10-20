package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        return "Даёт " + (hp / 2) + " золотых сердец на " + dur + " сек. Перезарядка: " + cd + " сек.";
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

        int amplifier = Math.max(0, (hp / 4) - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, amplifier, false, true, true));

        player.sendOverlayMessage(Component.literal("§eВторое дыхание. §f+" + (hp / 2) + " сердец."));
        return true;
    }
}