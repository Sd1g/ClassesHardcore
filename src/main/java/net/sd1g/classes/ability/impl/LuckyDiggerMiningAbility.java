package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HudMessagePayload;

import java.util.List;

public class LuckyDiggerMiningAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_LEVEL = new java.util.HashMap<>();

    @Override public String getId() { return "mining_mastery"; }
    @Override public String getDisplayName() { return "Мастерство шахтёра"; }
    @Override public boolean isPassive() { return true; }

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
        int last = LAST_LEVEL.getOrDefault(player.getUUID(), 0);

        // Эпичный эффект при повышении уровня
        if (data.luckyDiggerLevel > last) {
            ServerLevel level = player.level();
            ServerPlayNetworking.send(player, new HudMessagePayload("§aШахтёр: Уровень " + data.luckyDiggerLevel + "!", 0xFF55FF55, 80));

            // Триумфальные звуки
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);

            // Эпичные эффекты (золотые искры, фейерверк, чары)
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 80, 0.5, 1.0, 0.5, 0.2);
            level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 40, 1.0, 1.0, 1.0, 0.2);
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 60, 1.0, 1.5, 1.0, 0.5);
        }
        LAST_LEVEL.put(player.getUUID(), data.luckyDiggerLevel);

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