package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class HealingBrewAbility implements Ability {
    @Override public String getId() { return "healing_brew"; }
    @Override public String getDisplayName() { return "Лечебный отвар"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/healing_brew"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("healing_brew", 600);
    }

    @Override
    public String getDescription() {
        int cd = getBaseCooldown() / 20;
        return "Снимает все негативные эффекты. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Очищает дебаффы");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        List<MobEffectInstance> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) {
                effectsToRemove.add(effect);
            }
        }

        for (MobEffectInstance effect : effectsToRemove) {
            player.removeEffect(effect.getEffect());
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Эффект как у лечения
        level.sendParticles(ParticleTypes.HEART, x, y + 0.2, z, 8, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y - 0.5, z, 25, 0.6, 0.6, 0.6, 0.02);

        // Звук бульканья воды
        level.playSound(null, x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8f, 1.5f);

        player.sendOverlayMessage(Component.literal("§aЛечебный отвар применен!"));
        return true;
    }
}