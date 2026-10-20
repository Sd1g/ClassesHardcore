package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class GluttonyDepletionAbility implements Ability {
    @Override public String getId() { return "depletion"; }
    @Override public String getDisplayName() { return "Истощение"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("depletion", 1000);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400) / 20;
        int cd = cfg.getCooldown("depletion", 1000) / 20;
        return "Следующий удар накладывает Голод " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Голод V", "§7Длительность: §f20 сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.gluttonyDepletionArmed = true;
        PlayerClassData.markDirty(player);
        player.sendOverlayMessage(Component.literal("§4Истощение активировано."));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.gluttonyDepletionArmed) return;
        data.gluttonyDepletionArmed = false;
        PlayerClassData.markDirty(attacker);

        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4);
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400);

        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, amp, false, true));
    }
}