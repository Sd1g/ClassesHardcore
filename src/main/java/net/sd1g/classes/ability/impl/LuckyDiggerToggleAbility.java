package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.network.LuckyDiggerSyncPayload;

import java.util.List;

public class LuckyDiggerToggleAbility implements Ability {
    @Override public String getId() { return "toggle_bonus"; }
    @Override public String getDisplayName() { return "Смена бонуса"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/toggle_bonus"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("digger_toggle_cooldown", 600);
    }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("digger_toggle_cooldown", 600) / 20;
        return "Переключает бонус добычи между Ускорением копания и Удачей. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        String mode = AbilityClientState.luckyDiggerMode;
        return List.of("§7Текущий режим: " + ("efficiency".equals(mode) ? "§bЭффективность" : "§aУдача"));
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.luckyDiggerMode = "efficiency".equals(data.luckyDiggerMode) ? "luck" : "efficiency";
        PlayerClassData.markDirty(player);

        LuckyDiggerMiningAbility.applyCurrentEffect(player);
        ServerPlayNetworking.send(player, new LuckyDiggerSyncPayload(data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
        ServerPlayNetworking.send(player, new ActiveTimerPayload("toggle_bonus", getBaseCooldown()));

        ServerLevel level = player.level();

        // Приятный эффект переключения (чары и искры)
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.4, 0.4, 0.02);

        // ИСПРАВЛЕНИЕ ЗВУКА: Комбинация из трех звуков для сочности
        // 1. Основной громкий звук переключения баффа (Маяк)
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.2f);
        // 2. Магический перезвон (Аметист)
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        // 3. Звук подбора опыта для завершенности
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.0f);

        player.sendOverlayMessage(Component.literal("§aРежим: " + ("luck".equals(data.luckyDiggerMode) ? "Удача" : "Эффективность")));
        return true;
    }
}