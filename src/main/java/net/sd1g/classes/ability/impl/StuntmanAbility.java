package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class StuntmanAbility implements Ability {
    @Override public String getId() { return "fall_resistance"; }
    @Override public String getDisplayName() { return "Страховка"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.3333);
        double times = mult > 0 ? (1.0 / mult) : 1.0;
        String timesText = times == Math.floor(times)
                ? String.valueOf((int) times)
                : String.format("%.1f", times);
        return "Получает в " + timesText + " раза меньше урона от падения.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.3333);
        int reduction = (int) Math.round((1.0 - mult) * 100);
        return List.of("§7Снижение урона от падения: §a" + reduction + "%");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (player.tickCount % 12 == 0) {
            level.sendParticles(new DustParticleOptions(0xFFD700, 0.9f),
                    x, y + 0.05, z, 3, 0.4, 0.03, 0.4, 0.02);
        }
        if (player.tickCount % 20 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.4 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0xFFFFFF, 0.7f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        if (player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.CRIT, x, y + 1.0, z, 2, 0.4, 0.3, 0.4, 0.03);
        }
    }
}