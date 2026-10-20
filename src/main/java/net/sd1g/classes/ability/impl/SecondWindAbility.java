package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class SecondWindAbility implements Ability {
    @Override public String getId() { return "second_wind"; }
    @Override public String getDisplayName() { return "Второе дыхание"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 900; } // 45 сек

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/second_wind"); }

    @Override
    public String getDescription() {
        return "Получает 4 НР поглощения на 15 секунд. Перезарядка: 45 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Поглощение: §6+4 HP", "§7Длительность: §f15 сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0, false, true, true)); // 15 сек, 4 HP (амп 0)

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 20, 0.6, 0.8, 0.6, 0.4);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.3f);

        player.sendOverlayMessage(Component.literal("§eВторое дыхание. §f+2 сердца на 15 сек."));
        return true;
    }
}