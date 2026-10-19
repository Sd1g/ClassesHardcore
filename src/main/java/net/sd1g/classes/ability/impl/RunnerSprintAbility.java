package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.RunnerSyncPayload;

import java.util.List;

public class RunnerSprintAbility implements Ability {
    @Override public String getId() { return "runner_sprint"; }
    @Override public String getDisplayName() { return "Рывок"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 800; }
    @Override public int getTickInterval() { return 1; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("runner_sprint_duration_ticks", 300) / 20;
        int amp = cfg.getIntEffect("runner_sprint_amplifier", 4);
        return "Временно даёт Скорость " + toRoman(amp + 1) + " на " + dur + " секунд.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("runner_sprint_duration_ticks", 300);
        int amp = cfg.getIntEffect("runner_sprint_amplifier", 4);
        int cd = cfg.getCooldown("runner_sprint", getBaseCooldown());
        return List.of(
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Эффект: §bСкорость " + toRoman(amp + 1),
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.runnerSprintTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        ModConfig cfg = ModConfig.getInstance();
        int duration = cfg.getIntEffect("runner_sprint_duration_ticks", 300);
        int amplifier = cfg.getIntEffect("runner_sprint_amplifier", 4);

        data.runnerSprintTicks = duration;
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, amplifier, false, false, true));
        ServerPlayNetworking.send(player, new RunnerSyncPayload(duration));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Синий вихрь
        int points = 22;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.3;
            double rz = z + Math.sin(angle) * 1.3;
            level.sendParticles(new DustParticleOptions(0x4D99FF, 1.5f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        for (int i = 0; i < 8; i++) {
            level.sendParticles(new DustParticleOptions(0x80E6FF, 1.2f),
                    x, y + 0.2 + i * 0.2, z, 1, 0.15, 0, 0.15, 0);
        }
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.3, z, 30, 0.6, 0.3, 0.6, 0.25);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.3, z, 20, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(new DustParticleOptions(0x4D99FF, 1.8f), x, y + 0.5, z, 25, 0.6, 0.4, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0x80E6FF, 1.5f), x, y + 1.0, z, 18, 0.6, 0.4, 0.6, 0.1);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.0, z, 20, 0.5, 0.4, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.8, z, 6, 0.5, 0.3, 0.5, 0.0);

        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.TRIDENT_THUNDER,
                SoundSource.PLAYERS, 1.2f, 1.6f);

        int secondsLeft = duration / 20;
        player.sendOverlayMessage(Component.literal(
                "§bРывок. §fСкорость " + toRoman(amplifier + 1)
                        + ". До конца способности " + secondsLeft + " сек."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks <= 0) return;

        data.runnerSprintTicks--;

        if (data.runnerSprintTicks <= 0) {
            data.runnerSprintTicks = 0;
            RunnerPassiveAbility.applyPassiveSpeed(player);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new RunnerSyncPayload(0));

            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            // Завершение — синяя вспышка
            level.sendParticles(new DustParticleOptions(0x4D99FF, 1.5f), x, y + 0.5, z, 20, 0.6, 0.4, 0.6, 0.1);
            level.sendParticles(ParticleTypes.CLOUD, x, y + 0.3, z, 15, 0.5, 0.3, 0.5, 0.1);
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 8, 0.4, 0.4, 0.4, 0.05);

            level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST,
                    SoundSource.PLAYERS, 1.0f, 1.8f);

            player.sendOverlayMessage(Component.literal("§bРывок закончился"));
            return;
        }

        // ---- Аура длительности ----
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (data.runnerSprintTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, x, y + 0.2, z, 3, 0.35, 0.15, 0.35, 0.05);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.2, z, 2, 0.3, 0.1, 0.3, 0.02);
        }
        if (data.runnerSprintTicks % 6 == 0) {
            level.sendParticles(new DustParticleOptions(0x4D99FF, 1.0f), x, y + 0.4, z, 3, 0.4, 0.2, 0.4, 0.05);
        }

        if (data.runnerSprintTicks % 20 == 0) {
            int amplifier = ModConfig.getInstance().getIntEffect("runner_sprint_amplifier", 4);
            int secondsLeft = data.runnerSprintTicks / 20;
            player.sendOverlayMessage(Component.literal(
                    "§bРывок. §fСкорость " + toRoman(amplifier + 1)
                            + ". До конца способности " + secondsLeft + " сек."));
        }
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) {
            data.runnerSprintTicks = 0;
            player.removeEffect(MobEffects.SPEED);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new RunnerSyncPayload(0));
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}