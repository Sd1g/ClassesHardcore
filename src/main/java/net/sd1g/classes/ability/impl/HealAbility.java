package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class HealAbility implements Ability {
    @Override public String getId() { return "heal"; }
    @Override public String getDisplayName() { return "Исцеление"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("warrior_heal_cooldown", 440);
    }

    private float getHealAmount(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        float base = (float) cfg.getEffect("warrior_heal_amount", 4.0);
        int killsPerStep = cfg.getIntEffect("warrior_veteran_kills_per_step", 2000);
        float bonusPerStep = (float) cfg.getEffect("warrior_veteran_bonus_hp", 0.5);
        float maxBonus = (float) cfg.getEffect("warrior_veteran_max_bonus", 10.0);

        PlayerClassData data = PlayerClassData.get(player);
        float bonus = Math.min((data.warriorSwordKills / (float)killsPerStep) * bonusPerStep, maxBonus);
        return base + bonus;
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        float amt = (float) cfg.getEffect("warrior_heal_amount", 4.0);
        int regenAmp = cfg.getIntEffect("warrior_heal_regen_amp", 0) + 1;
        int regenTicks = cfg.getIntEffect("warrior_heal_regen_ticks", 100);
        int cd = cfg.getCooldown("warrior_heal_cooldown", 440) / 20;
        return "Восстанавливает " + amt + " НР и накладывает Регенерацию " + regenAmp + " на " + (regenTicks/20) + " секунд. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        if (player instanceof ServerPlayer sp) {
            ModConfig cfg = ModConfig.getInstance();
            float amount = getHealAmount(sp);
            int kills = PlayerClassData.get(sp).warriorSwordKills;
            int killsPerStep = cfg.getIntEffect("warrior_veteran_kills_per_step", 2000);
            float bonusHP = amount - (float) cfg.getEffect("warrior_heal_amount", 4.0);
            int toNext = killsPerStep - (kills % killsPerStep);

            return List.of(
                    "§7Мгновенное лечение: §a" + amount + " HP",
                    "§7Убийств мечом: §b" + kills,
                    "§7Текущий бонус: §a+" + bonusHP + " HP",
                    (toNext > 0 ? "§7До след. бонуса: §f" + toNext + " убийств" : "§7Максимальный бонус")
            );
        }
        return List.of();
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        float amount = getHealAmount(player);
        int regenTicks = cfg.getIntEffect("warrior_heal_regen_ticks", 100);
        int regenAmp = cfg.getIntEffect("warrior_heal_regen_amp", 0);

        player.heal(amount);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp));

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1f, 1.5f);
        player.sendOverlayMessage(Component.literal("§aИсцеление. §f+" + amount + " HP."));
        return true;
    }
}