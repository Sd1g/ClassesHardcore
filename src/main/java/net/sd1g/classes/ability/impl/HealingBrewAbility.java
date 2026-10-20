package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class HealingBrewAbility implements Ability {
    @Override public String getId() { return "healing_brew"; }
    @Override public String getDisplayName() { return "Лечебный отвар"; }
    @Override public boolean isPassive() { return false; }

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
        // Создаем копию списка, чтобы избежать ConcurrentModificationException
        List<MobEffectInstance> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) {
                effectsToRemove.add(effect);
            }
        }

        for (MobEffectInstance effect : effectsToRemove) {
            player.removeEffect(effect.getEffect());
        }

        player.sendOverlayMessage(Component.literal("§aЛечебный отвар применен!"));
        return true;
    }
}