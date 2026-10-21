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

public class PorcupineChitinAbility implements Ability {
    @Override public String getId() { return "porcupine_chitin"; }
    @Override public String getDisplayName() { return "Хитиновый покров"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_chitin"); }

    @Override
    public String getDescription() {
        // ИСПРАВЛЕНО: Динамический расчет процента из конфига
        double mult = ModConfig.getInstance().getEffect("porcupine_chitin_damage_multiplier", 0.8);
        int reduction = (int) Math.round((1.0 - mult) * 100);
        return "Постоянно получает на " + reduction + "% меньше входящего урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double mult = ModConfig.getInstance().getEffect("porcupine_chitin_damage_multiplier", 0.8);
        int reduction = (int) Math.round((1.0 - mult) * 100);
        return List.of("§7Снижение урона: §a" + reduction + "%");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();

        if (player.tickCount % 5 == 0) {
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            double angle = (player.tickCount * 0.15) % (Math.PI * 2);
            double radius = 0.7;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            level.sendParticles(new DustParticleOptions(0x8B4513, 1.2f), px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.4, 0.4, 0.4, 0.02);
        }

        if (player.tickCount % 40 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.3f, 1.2f);
        }
    }
}