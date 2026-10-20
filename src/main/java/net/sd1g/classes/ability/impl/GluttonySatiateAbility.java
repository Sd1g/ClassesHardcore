package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class GluttonySatiateAbility implements Ability {
    @Override public String getId() { return "satiate"; }
    @Override public String getDisplayName() { return "Утоление"; }
    @Override public boolean isPassive() { return false; }

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

        player.sendOverlayMessage(Component.literal("§6Утоление. §f+" + amount + " сытости."));
        return true;
    }
}