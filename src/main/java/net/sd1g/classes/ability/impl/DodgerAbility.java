package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class DodgerAbility implements Ability {
    @Override public String getId() { return "dodge_chance"; }
    @Override public String getDisplayName() { return "Ловкость"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(
                ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20) * 100);
        return "С шансом " + chance + "% уворачивается от атак — урон не наносится.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20) * 100);
        return List.of("§7Шанс уклонения: §a" + chance + "%");
    }

    /**
     * Вызывается через ClassesHardcore.ALLOW_DAMAGE — там мы уже отменили урон.
     * Здесь только визуал + звук при успешном уклонении.
     */
    public static void playDodgeEffect(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Быстрая вспышка — циан и белый, как мгновенный рывок в сторону
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 4, 0.4, 0.2, 0.4, 0.0);
        level.sendParticles(new DustParticleOptions(0x4DFFFF, 1.4f), x, y, z, 12, 0.5, 0.4, 0.5, 0.15);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.1f), x, y, z, 8, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(ParticleTypes.CLOUD, x, y - 0.5, z, 6, 0.3, 0.15, 0.3, 0.05);

        // Звук срабатывания — 2 коротких сигнала
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.8f);
        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 0.9f, 1.7f);
    }
}