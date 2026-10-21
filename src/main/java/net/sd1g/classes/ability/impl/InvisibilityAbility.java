package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.network.InvisibilitySyncPayload;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class InvisibilityAbility implements Ability {
    @Override public String getId() { return "invisibility"; }
    @Override public String getDisplayName() { return "Невидимость"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/invisibility"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("invisibility", 1200);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("invisibility_duration_ticks", 100) / 20;
        int cd = cfg.getCooldown("invisibility", 1200) / 20;

        // Динамический расчет множителя урона из конфига (по умолчанию 3.0 = +200%)
        double mult = cfg.getEffect("ambush_damage_multiplier", 3.0);
        int bonusPercent = (int) Math.round((mult - 1.0) * 100);

        // Динамический расчет чистого урона из конфига
        double pureDmg = cfg.getEffect("ambush_pure_damage", 2.0) / 2.0;

        return "Невидимость на " + dur + " сек. Первый удар наносит +" + bonusPercent + "% урона + " + pureDmg + " НР чистого урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Длительность: §f5 сек.", "§7Кулдаун: §f60 сек.");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.invisibilityActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        int duration = ModConfig.getInstance().getIntEffect("invisibility_duration_ticks", 100);
        PlayerClassData data = PlayerClassData.get(player);
        data.invisibilityActiveTicks = duration;
        data.invisibleAmbushArmed = true;
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
        ServerPlayNetworking.send(player, new InvisibilitySyncPayload(duration));
        ServerPlayNetworking.send(player, new ActiveTimerPayload("invisibility", duration));

        // Эффект исчезновения
        spawnInvisibilityEffect(player.level(), player, false);

        player.sendOverlayMessage(Component.literal("§bНевидимость на " + (duration/20) + " сек."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks <= 0) return;

        data.invisibilityActiveTicks--;

        if (data.invisibilityActiveTicks <= 0) {
            data.invisibleAmbushArmed = false;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));

            // Эффект появления
            spawnInvisibilityEffect(player.level(), player, true);

            player.sendOverlayMessage(Component.literal("§bНевидимость закончилась"));
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.invisibilityActiveTicks > 0 && data.invisibleAmbushArmed) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            attacker.removeEffect(MobEffects.INVISIBILITY);
            attacker.setInvisible(false);
            PlayerClassData.markDirty(attacker);
            ServerPlayNetworking.send(attacker, new InvisibilitySyncPayload(0));

            float pureDmg = (float) ModConfig.getInstance().getEffect("ambush_pure_damage", 2.0);
            PureDamage.apply(attacker, target, pureDmg);

            ServerLevel level = attacker.level();
            double x = target.getX();
            double y = target.getY() + (target.getBbHeight() / 2.0);
            double z = target.getZ();

            // ==========================================
            // ЭПИЧНЫЙ ЭФФЕКТ УДАРА ИЗ ТЕНИ
            // ==========================================

            level.sendParticles(new DustParticleOptions(0x000000, 2.5f), x, y, z, 60, 0.6, 0.6, 0.6, 0.2);
            level.sendParticles(new DustParticleOptions(0x4B0082, 2.0f), x, y, z, 40, 0.5, 0.5, 0.5, 0.15);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 4, 0.3, 0.3, 0.3, 0.0);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.4, 0.4, 0.4, 0.2);
            level.sendParticles(ParticleTypes.SOUL, x, y, z, 15, 0.3, 0.4, 0.3, 0.05);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 10, 0.2, 0.2, 0.2, 0.1);

            level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, x, y, z, SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.6f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

            attacker.sendOverlayMessage(Component.literal("§cВы раскрыты!"));
        }
    }

    public static void spawnInvisibilityEffect(ServerLevel level, ServerPlayer player, boolean isAppearing) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 15, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(new DustParticleOptions(0x8A2BE2, 1.5f), x, y, z, 30, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.2, 0.2, 0.2, 0.05);

        if (isAppearing) {
            level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
        } else {
            level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks > 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            player.removeEffect(MobEffects.INVISIBILITY);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));
            spawnInvisibilityEffect(player.level(), player, true);
        }
    }
}