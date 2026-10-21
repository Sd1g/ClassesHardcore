package net.sd1g.classes.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectHelper {

    // ==================== ЭФФЕКТЫ НА ИГРОКЕ (БАФФЫ/ЛЕЧЕНИЕ) ====================

    public static void playHealEffect(ServerLevel level, Entity entity, float amount) {
        // Зеленые искры и сердечки для лечения
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1, entity.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
        if (amount >= 2.0f) {
            level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1.2, entity.getZ(), (int)(amount / 2), 0.3, 0.3, 0.3, 0.0);
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    public static void playSelfBuffEffect(ServerLevel level, Entity entity) {
        // Восходящий вихрь (для скорости, монолита и т.д.)
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.2, entity.getZ(), 20, 0.4, 0.2, 0.4, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 0.5, entity.getZ(), 30, 0.5, 1.0, 0.5, 0.5);
    }

    // ==================== ЭФФЕКТЫ НА ЦЕЛИ (УРОН/ДЕБАФФЫ) ====================

    public static void playHitEffect(ServerLevel level, Entity target) {
        // Критические искры при ударе
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
    }

    public static void playDebuffEffect(ServerLevel level, Entity target, ParticleOptions particle, SoundEvent sound) {
        // Магическое облако вокруг цели
        level.sendParticles(particle, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    // ==================== СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ====================

    public static void playMonolithEffect(ServerLevel level, LivingEntity player, int durationTicks) {
        // Эффект из картинки: черный дым + фиолетовые искры (портал) + темная аура
        for (int i = 0; i < 4; i++) {
            double angle = (i * Math.PI / 2) + (level.getGameTime() * 0.05);
            double radius = 1.2;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            // Черный дым
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, player.getY() + 0.1, z, 5, 0.1, 0.5, 0.1, 0.01);
            // Фиолетовые частицы портала
            level.sendParticles(ParticleTypes.PORTAL, x, player.getY() + 0.5, z, 10, 0.2, 0.8, 0.2, 0.2);
        }

        // Центральный всплеск
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.5f); // Глухой каменный звук
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.5f); // Басовитый гул
    }

    public static void playVampireDrainEffect(ServerLevel level, Entity attacker, Entity target) {
        // Урон - на цели, Хил - на игроке
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.6f, 1.2f);
    }
}