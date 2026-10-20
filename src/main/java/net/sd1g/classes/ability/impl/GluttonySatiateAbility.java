package net.sd1g.classes.ability.impl;

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

import java.util.List;

public class GluttonySatiateAbility implements Ability {
    @Override public String getId() { return "satiate"; }
    @Override public String getDisplayName() { return "Утоление"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; } // 30 сек

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/satiate"); }

    @Override
    public String getDescription() {
        return "Мгновенно восстанавливает 4 единицы сытости. Перезарядка: 30 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Сытость: §a+4");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        var foodData = player.getFoodData();
        int newLevel = Math.min(20, foodData.getFoodLevel() + 4);
        foodData.setFoodLevel(newLevel);
        foodData.setSaturation(Math.min(foodData.getSaturationLevel() + 4, newLevel));

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.6, 0.5, 0.6, 0.06);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.3f, 1.3f);

        player.sendOverlayMessage(Component.literal("§6Утоление. §f+4 сытости."));
        return true;
    }
}