package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class MonolithAbility implements Ability {
    @Override public String getId() { return "monolith"; }
    @Override public String getDisplayName() { return "Монолит"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("monolith", 1200);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3) + 1;
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100) / 20;
        int cd = getBaseCooldown() / 20;
        return "Сопротивление " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Заменяет пассивное Сопротивление");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3);
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100);

        player.removeEffect(MobEffects.RESISTANCE);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur, amp, false, true, true));
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§bМонолит! §fСопротивление " + (amp + 1)));
        return true;
    }
}