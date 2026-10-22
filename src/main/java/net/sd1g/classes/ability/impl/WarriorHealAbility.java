package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.Progression;

public class WarriorHealAbility implements Ability {
    @Override public String getId() { return "warrior_heal"; }
    @Override public String getDisplayName() { return "Исцеление"; }
    @Override public boolean isPassive() { return false; }
    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/heal"); }

    @Override
    public int getBaseCooldownTicks() {
        return ModConfig.getInstance().getTicks("warrior", "warrior_heal.cooldown_ticks", 440);
    }

    @Override
    public int getCastTimeTicks(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int base = cfg.getTicks("warrior", "warrior_heal.base_cast_time_ticks", 0);
        if (player == null) return base;

        int kills = PlayerClassData.get(player).warriorSwordKills;
        double perLevel = cfg.getInt("warrior", "warrior_veteran.kills_per_step", 2000);
        double perLevelBonus = cfg.get("warrior", "warrior_veteran.cast_time_per_step_ticks", 4.0);
        double maxBonus = cfg.get("warrior", "warrior_veteran.max_cast_time_bonus_ticks", 80.0);

        return base + (int) Progression.calculate(kills, perLevel, perLevelBonus, maxBonus).bonus();
    }

    private double getHealAmount(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double base = cfg.get("warrior", "warrior_heal.amount_hp", 4.0);
        if (player == null) return base;

        int kills = PlayerClassData.get(player).warriorSwordKills;
        double perLevel = cfg.getInt("warrior", "warrior_veteran.kills_per_step", 2000);
        double perLevelBonus = cfg.get("warrior", "warrior_veteran.hp_per_step", 0.5);
        double maxBonus = cfg.get("warrior", "warrior_veteran.max_hp_bonus", 10.0);

        return base + Progression.calculate(kills, perLevel, perLevelBonus, maxBonus).bonus();
    }

    @Override
    public TooltipData buildTooltip(Player player) {
        ModConfig cfg = ModConfig.getInstance();

        int kills = 0;
        if (player instanceof ServerPlayer sp) {
            kills = PlayerClassData.get(sp).warriorSwordKills;
        } else if (player != null) {
            kills = AbilityClientState.warriorSwordKills;
        }

        double perLevel = cfg.getInt("warrior", "warrior_veteran.kills_per_step", 2000);
        double perLevelBonus = cfg.get("warrior", "warrior_veteran.hp_per_step", 0.5);
        double maxBonus = cfg.get("warrior", "warrior_veteran.max_hp_bonus", 10.0);
        double baseHeal = cfg.get("warrior", "warrior_heal.amount_hp", 4.0);

        Progression prog = Progression.calculate(kills, perLevel, perLevelBonus, maxBonus);
        double totalHeal = baseHeal + prog.bonus();

        int baseCast = cfg.getTicks("warrior", "warrior_heal.base_cast_time_ticks", 0);
        double castPerLevel = cfg.get("warrior", "warrior_veteran.cast_time_per_step_ticks", 4.0);
        double maxCastBonus = cfg.get("warrior", "warrior_veteran.max_cast_time_bonus_ticks", 80.0);
        int castTime = baseCast + (int) Progression.calculate(kills, perLevel, castPerLevel, maxCastBonus).bonus();

        // Исправлено: убран двойной плюс, добавлено отображение каста в perLevel
        return TooltipData.builder("Восстанавливает здоровье и накладывает Регенерацию I на 5 сек.")
                .castTime(castTime)
                .cooldown(getBaseCooldownTicks())
                .duration(cfg.getTicks("warrior", "warrior_heal.regen_duration_ticks", 100))
                .currentBonus(String.format("%.1f HP", totalHeal))
                .toNext(prog.toNext() > 0 ? prog.toNext() + " убийств" : "Максимум")
                .perLevel("+" + perLevelBonus + " HP, +" + String.format("%.1f", castPerLevel / 20.0) + "с каста")
                .required((int)perLevel + " убийств")
                .build();
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        float amount = (float) getHealAmount(player);
        int regenTicks = cfg.getTicks("warrior", "warrior_heal.regen_duration_ticks", 100);
        int regenAmp = cfg.getInt("warrior", "warrior_heal.regen_amplifier", 0);

        player.heal(amount);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp));

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.6, 0.6, 0.6, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);

        return true;
    }
}