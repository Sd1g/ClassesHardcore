package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.VampireSyncPayload;

import java.util.List;

public class VampireDrainAbility implements Ability {
    @Override public String getId() { return "vampire_drain"; }
    @Override public String getDisplayName() { return "Вампиризм"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1200; }
    @Override public int getTickInterval() { return 1; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("vampire_active_duration_ticks", 200) / 20;
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.60) * 100);
        return "На " + dur + " сек каждый удар лечит на " + percent + "% от нанесённого урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("vampire_active_duration_ticks", 200);
        int cd = cfg.getCooldown("vampire_drain", getBaseCooldown());
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.60) * 100);
        return List.of(
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Лечение: §a" + percent + "% §7от урона",
                "§7Шанс срабатывания: §a100%",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.vampireActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        int duration = ModConfig.getInstance().getIntEffect("vampire_active_duration_ticks", 200);
        data.vampireActiveTicks = duration;
        PlayerClassData.markDirty(player);
        ServerPlayNetworking.send(player, new VampireSyncPayload(duration));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // ---- Активация: тёмно-красный вихрь ----
        int points = 22;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            level.sendParticles(new DustParticleOptions(0xB30000, 1.5f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        for (int i = 0; i < 8; i++) {
            level.sendParticles(new DustParticleOptions(0xB30000, 1.3f),
                    x, y + 0.3 + i * 0.2, z, 1, 0.15, 0, 0.15, 0);
        }
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y + 1.0, z, 30, 0.6, 0.7, 0.6, 0.04);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 1.0, z, 20, 0.6, 0.6, 0.6, 0.08);
        level.sendParticles(new DustParticleOptions(0x4D0000, 1.8f), x, y + 0.7, z, 20, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0xFF1A1A, 1.5f), x, y + 1.3, z, 15, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 10, 0.5, 0.6, 0.5, 0.2);

        level.playSound(null, x, y, z, SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.PLAYERS, 1.4f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.PLAYERS, 1.2f, 1.4f);

        int secondsLeft = duration / 20;
        player.sendOverlayMessage(Component.literal(
                "§cВампиризм. §fДо конца способности " + secondsLeft + " сек."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks <= 0) return;

        data.vampireActiveTicks--;

        if (data.vampireActiveTicks <= 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));

            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            // ---- Завершение: мягкая вспышка ----
            level.sendParticles(ParticleTypes.POOF, x, y, z, 15, 0.6, 0.6, 0.6, 0.05);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 20, 0.6, 0.6, 0.6, 0.03);
            level.sendParticles(new DustParticleOptions(0xB30000, 1.3f), x, y, z, 15, 0.5, 0.5, 0.5, 0.08);

            level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 0.8f, 1.0f);

            player.sendOverlayMessage(Component.literal("§cВампиризм закончился"));
            return;
        }

        // ---- Аура активной способности ----
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Красные искры у ног раз в 3 тика
        if (data.vampireActiveTicks % 3 == 0) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 1.0, z, 3, 0.5, 0.5, 0.5, 0.03);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y + 1.0, z, 4, 0.6, 0.6, 0.6, 0.02);
        }
        // Красные кольца раз в 6 тиков
        if (data.vampireActiveTicks % 6 == 0) {
            level.sendParticles(new DustParticleOptions(0xFF1A1A, 1.2f),
                    x, y + 0.8, z, 4, 0.5, 0.3, 0.5, 0.05);
        }
        // Столб тёмных частиц раз в 12 тиков
        if (data.vampireActiveTicks % 12 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0x4D0000, 0.9f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }

        // Раз в секунду — обновление таймера + heartbeat
        if (data.vampireActiveTicks % 20 == 0) {
            int secondsLeft = data.vampireActiveTicks / 20;
            player.sendOverlayMessage(Component.literal(
                    "§cВампиризм. §fДо конца способности " + secondsLeft + " сек."));
            level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks <= 0) return;

        double percent = ModConfig.getInstance().getEffect("vampire_heal_percent", 0.60);
        attacker.heal((float) (damageDealt * percent));

        ServerLevel level = attacker.level();
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() * 0.5;
        double tz = target.getZ();

        // Частицы от цели к атакующему (визуально — вспышка у цели)
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tx, ty, tz, 10, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, tx, ty, tz, 8, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(new DustParticleOptions(0xFF0000, 1.3f), tx, ty, tz, 10, 0.4, 0.4, 0.4, 0.1);
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks > 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));
        }
    }
}