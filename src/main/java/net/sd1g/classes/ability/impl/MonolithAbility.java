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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;

import java.util.List;

public class MonolithAbility implements Ability {
    @Override public String getId() { return "monolith"; }
    @Override public String getDisplayName() { return "Монолит"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/monolith"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("monolith", 1200);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3) + 1;
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100) / 20;
        int cd = getBaseCooldown() / 20;
        return "Сопротивление " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Заменяет пассивное Сопротивление");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("monolith_resistance_amplifier", 3);
        int dur = cfg.getIntEffect("monolith_duration_ticks", 100);

        player.removeEffect(MobEffects.RESISTANCE);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur, amp, false, true, true));

        ServerPlayNetworking.send(player, new ActiveTimerPayload("monolith", dur));

        // Устанавливаем таймер активности для ауры
        PlayerClassData data = PlayerClassData.get(player);
        data.setCooldown("monolith_active", player.level().getGameTime(), dur);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Первоначальный взрыв пыли при активации
        level.sendParticles(new DustParticleOptions(0x808080, 2.5f), x, y, z, 60, 0.8, 1.0, 0.8, 0.1);
        level.sendParticles(new DustParticleOptions(0xA9A9A9, 2.0f), x, y, z, 40, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.ASH, x, y, z, 40, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.5, 0.2, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, x, y, z, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);

        player.sendOverlayMessage(Component.literal("§bМонолит! §fСопротивление " + (amp + 1)));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        // Если способность активна (таймер не истек) - крутим ауру
        if (data.isOnCooldown("monolith_active", now)) {
            // Спавним частицы раз в 2 тика для оптимизации
            if (player.tickCount % 2 == 0) {
                ServerLevel level = player.level();

                // Вычисляем угол по кругу
                double angle = (now * 0.2) % (Math.PI * 2);
                double radius = 1.2;

                // Позиция по кругу с легким покачиванием вверх-вниз
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + 1.0 + Math.sin(now * 0.1) * 0.3;

                // Серые частицы пыли и пепла вокруг игрока
                level.sendParticles(new DustParticleOptions(0x808080, 1.5f), x, y, z, 2, 0.05, 0.05, 0.05, 0.0);
                level.sendParticles(ParticleTypes.ASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}