package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class GluttonySatiateAbility implements Ability {
    @Override public String getId() { return "satiate"; }
    @Override public String getDisplayName() { return "Утоление"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/satiate"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("satiate", 600);
    }

    @Override
    public String getDescription() {
        int amount = ModConfig.getInstance().getIntEffect("satiate_food_amount", 4);
        int cd = ModConfig.getInstance().getCooldown("satiate", 600) / 20;
        return "Восстанавливает " + amount + " единицы сытости. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amount = ModConfig.getInstance().getIntEffect("satiate_food_amount", 4);
        return List.of("§7Сытость: §a+" + amount);
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        int amount = ModConfig.getInstance().getIntEffect("satiate_food_amount", 4);
        var foodData = player.getFoodData();
        int newLevel = Math.min(20, foodData.getFoodLevel() + amount);
        foodData.setFoodLevel(newLevel);
        foodData.setSaturation(Math.min(foodData.getSaturationLevel() + amount, newLevel));

        ServerLevel level = player.level();

        // Эффект "Аппетитного пара и жара"
        // 1. Белый пар (как от горячей еды)
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
        // 2. Оранжевые/золотистые частицы (цвет жареной корочки)
        level.sendParticles(new DustParticleOptions(0xFF8C00, 1.5f), player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
        // 3. Белые искры для сочности
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.05);

        // Звуки (хруст и отрыжка) — оставляем как было
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.0f);

        player.sendOverlayMessage(Component.literal("§6Утоление. §f+" + amount + " сытости."));
        return true;
    }
}