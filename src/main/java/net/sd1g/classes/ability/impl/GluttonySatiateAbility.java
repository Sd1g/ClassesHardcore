package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class GluttonySatiateAbility implements Ability {
    @Override public String getId() { return "satiate"; }
    @Override public String getDisplayName() { return "Утоление"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amount = (int) cfg.getEffect("satiate_food_amount", 2.0);
        int cdSec = cfg.getCooldown("satiate", getBaseCooldown()) / 20;
        return "Мгновенно восстанавливает " + amount + " единицы сытости. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int amount = (int) cfg.getEffect("satiate_food_amount", 2.0);
        int cd = cfg.getCooldown("satiate", getBaseCooldown());
        return List.of(
                "§7Сытость: §a+" + amount,
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amount = (int) cfg.getEffect("satiate_food_amount", 2.0);

        var foodData = player.getFoodData();
        int newLevel = Math.min(20, foodData.getFoodLevel() + amount);
        foodData.setFoodLevel(newLevel);
        foodData.setSaturation(Math.min(foodData.getSaturationLevel() + amount, newLevel));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Восходящие золотые частицы еды
        for (int i = 0; i < 12; i++) {
            level.sendParticles(new DustParticleOptions(0xD9A400, 1.2f),
                    x, y + 0.3 + i * 0.12, z, 1, 0.25, 0, 0.25, 0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 20, 0.6, 0.5, 0.6, 0.06);
        level.sendParticles(ParticleTypes.HEART, x, y + 1.3, z, 5, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(new DustParticleOptions(0xD9A400, 1.5f), x, y + 1.0, z, 20, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(new DustParticleOptions(0xFFD580, 1.0f), x, y + 1.5, z, 12, 0.4, 0.4, 0.4, 0.03);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS, 1.3f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS, 1.2f, 1.0f);

        player.sendOverlayMessage(Component.literal("§6Утоление. §f+" + amount + " сытости."));
        return true;
    }
}