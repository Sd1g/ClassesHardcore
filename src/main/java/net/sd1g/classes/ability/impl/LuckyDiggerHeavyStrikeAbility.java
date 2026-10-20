package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class LuckyDiggerHeavyStrikeAbility implements Ability {
    @Override public String getId() { return "digger_heavy_strike"; }
    @Override public String getDisplayName() { return "Тяжёлый удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("digger_heavy_strike_chance", 0.05) * 100);
        int dur = cfg.getIntEffect("digger_heavy_strike_duration", 300) / 20;
        return "С вероятностью " + chance + "% накладывает Утомление V на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("digger_heavy_strike_chance", 0.05) * 100);
        return List.of("§7Шанс: §f" + chance + "%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damage) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("digger_heavy_strike_chance", 0.05);
        if (attacker.getRandom().nextDouble() < chance) {
            int dur = cfg.getIntEffect("digger_heavy_strike_duration", 300);
            int amp = cfg.getIntEffect("digger_heavy_strike_amp", 4);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, dur, amp, true, true));
            attacker.sendOverlayMessage(Component.literal("§6Тяжёлый удар! §fУтомление V."));
        }
    }
}