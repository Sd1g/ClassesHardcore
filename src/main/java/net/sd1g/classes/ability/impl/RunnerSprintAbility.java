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
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.network.RunnerSyncPayload;

import java.util.List;

public class RunnerSprintAbility implements Ability {
    @Override public String getId() { return "runner_sprint"; }
    @Override public String getDisplayName() { return "Рывок"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_sprint"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("runner_sprint", 800);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("runner_sprint_duration_ticks", 300) / 20;
        int amp = cfg.getIntEffect("runner_sprint_amplifier", 4) + 1;
        int cd = cfg.getCooldown("runner_sprint", 800) / 20;
        return "Скорость " + amp + " на " + dur + " секунд. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Кулдаун: §f40 сек.");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.runnerSprintTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("runner_sprint_duration_ticks", 300);
        int amp = cfg.getIntEffect("runner_sprint_amplifier", 4);

        PlayerClassData data = PlayerClassData.get(player);
        data.runnerSprintTicks = dur;
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, amp, false, false, true));
        ServerPlayNetworking.send(player, new RunnerSyncPayload(dur));
        ServerPlayNetworking.send(player, new ActiveTimerPayload("runner_sprint", dur));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 0.2;
        double z = player.getZ();

        // ЭПИЧНЫЙ ВЗРЫВ СКОРОСТИ
        // 1. Облака ветра и пыли
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 30, 0.5, 0.2, 0.5, 0.1);
        // 2. Голубой и белый шлейф (цвет скорости)
        level.sendParticles(new DustParticleOptions(0x00FFFF, 2.0f), x, y, z, 40, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.5f), x, y, z, 20, 0.4, 0.4, 0.4, 0.1);
        // 3. Визуальный разрез воздуха
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.5, z, 5, 0.4, 0.4, 0.4, 0.1);

        // ЗВУКИ СКОРОСТИ
        level.playSound(null, x, y, z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 2.0f);

        player.sendOverlayMessage(Component.literal("§bРывок активирован."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks <= 0) return;

        data.runnerSprintTicks--;

        // Постоянный шлейф во время действия рывка
        if (player.isSprinting() && player.tickCount % 2 == 0) {
            ServerLevel level = player.level();
            level.sendParticles(new DustParticleOptions(0x00FFFF, 1.0f),
                    player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
        }

        if (data.runnerSprintTicks <= 0) {
            data.runnerSprintTicks = 0;
            RunnerPassiveAbility.applyPassiveSpeed(player);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new RunnerSyncPayload(0));
        }
    }
}