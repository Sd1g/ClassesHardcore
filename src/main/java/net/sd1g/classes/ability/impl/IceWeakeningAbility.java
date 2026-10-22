package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class IceWeakeningAbility implements Ability {
    @Override public String getId() { return "ice_weakening"; }
    @Override public String getDisplayName() { return "Ледяное ослабление"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_weakening"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double reductionHP = cfg.getEffect("ice_weakening_damage_per_level", 1.0);
        return "Каждый уровень эффекта «Замедление» на противнике снижает его исходящий урон на " + reductionHP + " HP.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Логика обрабатывается через Mixin.");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();

        if (player.tickCount % 4 == 0) {
            double x = player.getX();
            double y = player.getY() + 0.5;
            double z = player.getZ();

            double angle = (player.tickCount * 0.1) % (Math.PI * 2);
            double radius = 1.2;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            level.sendParticles(new DustParticleOptions(0x87CEEB, 1.0f), px, y + 0.5, pz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.5, 0.2, 0.5, 0.01);
        }

        if (player.tickCount % 40 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.3f, 0.5f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 0.5f, 0.5f);
        }
    }
}