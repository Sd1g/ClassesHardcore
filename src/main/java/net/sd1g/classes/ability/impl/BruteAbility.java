package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class BruteAbility implements Ability {
    @Override public String getId() { return "brute_passive"; }
    @Override public String getDisplayName() { return "Мастер булавы"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/brute_passive"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int mace = (int) Math.round((cfg.getEffect("brute_mace_damage_multiplier", 1.5) - 1.0) * 100);
        int other = (int) Math.round((1.0 - cfg.getEffect("brute_other_damage_multiplier", 0.3)) * 100);
        return "Урон булавой +" + mace + "%. Другое оружие наносит на " + other + "% меньше.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Множители урона из balance.json");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        // Показываем, что это Громила, только когда он держит булаву
        if (player.getMainHandItem().is(Items.MACE)) {
            ServerLevel level = player.level();

            // Аура (темно-красная/багровая пыль и мелкие искры)
            if (player.tickCount % 4 == 0) {
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();

                double angle = (player.tickCount * 0.2) % (Math.PI * 2);
                double radius = 0.6;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;

                level.sendParticles(new DustParticleOptions(0x8B0000, 1.2f), px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.3, 0.3, 0.3, 0.02);
            }

            // Тяжелый звук (лязг брони или тяжелый вздох) раз в 2 секунды
            if (player.tickCount % 40 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.4f, 0.5f);
            }
        }
    }
}