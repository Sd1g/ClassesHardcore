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

public class DodgerAbility implements Ability {
    @Override public String getId() { return "dodger_passive"; }
    @Override public String getDisplayName() { return "Ловкость"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/dodger_passive"); }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20) * 100);
        return "С шансом " + chance + "% полностью избегает урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Базовый шанс уклонения");
    }

    // Этот метод вызывается из DodgerDodgeMixin
    public static void playDodgeEffect(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Эффект уворота (вспышка ветра и зеленые искры)
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 15, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f), x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // Звук уворота (резкий взмах)
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 2.0f);
    }
}