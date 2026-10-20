package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class LuckyDiggerMiningAbility implements Ability {
    @Override public String getId() { return "mining_mastery"; }
    @Override public String getDisplayName() { return "Мастерство шахтёра"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/mining_mastery"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int perLevel = cfg.getIntEffect("digger_ore_per_level", 200);
        int maxLevel = cfg.getIntEffect("digger_max_level", 4);
        return "За каждые " + perLevel + " руды +1 уровень (макс. " + maxLevel + ").";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int perLevel = cfg.getIntEffect("digger_ore_per_level", 200);
        int maxLevel = cfg.getIntEffect("digger_max_level", 4);

        String mode = AbilityClientState.luckyDiggerMode;
        int total = AbilityClientState.luckyDiggerTotalOre;
        int level = AbilityClientState.luckyDiggerLevel;

        int toNext = perLevel - (total % perLevel);
        if (total % perLevel == 0 && total > 0) toNext = perLevel;

        return List.of(
                "§7Добыто: §f" + total,
                "§7Уровень: §f" + level + (level >= maxLevel ? " §7(макс.)" : ""),
                "§7До след.: §f" + toNext,
                "§7Режим: " + ("efficiency".equals(mode) ? "§bЭффективность" : "§aУдача")
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.luckyDiggerLevel <= 0) return;

        if ("efficiency".equals(data.luckyDiggerMode)) {
            ModConfig cfg = ModConfig.getInstance();
            int maxAmp = cfg.getIntEffect("digger_haste_max_amp", 4);
            int amplifier = Math.min(data.luckyDiggerLevel - 1, maxAmp);
            MobEffectInstance cur = player.getEffect(MobEffects.HASTE);
            if (cur == null || cur.getAmplifier() != amplifier || cur.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
            }
        }
    }

    public static void applyCurrentEffect(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.LUCK);
        if (data.luckyDiggerLevel <= 0) return;

        int maxAmp = ModConfig.getInstance().getIntEffect("digger_haste_max_amp", 4);
        if ("efficiency".equals(data.luckyDiggerMode)) {
            int amplifier = Math.min(data.luckyDiggerLevel - 1, maxAmp);
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, amplifier, true, false));
        } else if ("luck".equals(data.luckyDiggerMode)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, Math.min(data.luckyDiggerLevel - 1, maxAmp), true, false));
        }
    }
}