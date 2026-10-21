package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class RunnerSpeedCritAbility implements Ability {
    @Override public String getId() { return "runner_speed_crit"; }
    @Override public String getDisplayName() { return "Скоростной крит"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_speed_crit"); }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("runner_crit_chance", 0.05) * 100);
        return "С шансом " + chance + "% наносит доп. урон в зависимости от разницы скорости.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает от разницы эффектов");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        if (attacker.getRandom().nextDouble() >= cfg.getEffect("runner_crit_chance", 0.05)) return;

        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();
        if (data.isOnCooldown("runner_crit_cd", now)) return;

        int diff = getSpeedTier(attacker) - getSpeedTier(target);

        if (diff >= 1 && diff <= 9) {
            double multiplier = cfg.getEffect("runner_crit_t" + diff + "_mult", 1.0);
            float pureDamage = (float) cfg.getEffect("runner_crit_t" + diff + "_dmg", 1.5);

            if (multiplier > 0) {
                data.setCooldown("runner_crit_cd", now, 10);
                target.hurtServer((net.minecraft.server.level.ServerLevel) attacker.level(), attacker.damageSources().playerAttack(attacker), damageDealt * (float)multiplier);
                PureDamage.apply(attacker, target, pureDamage);

                ServerLevel level = attacker.level();
                double x = target.getX();
                double y = target.getY() + (target.getBbHeight() / 2.0);
                double z = target.getZ();

                // ==========================================
                // МАСШТАБИРУЕМЫЕ ЭФФЕКТЫ ОТ РАЗНИЦЫ СКОРОСТЕЙ
                // ==========================================

                if (diff >= 1 && diff <= 3) {
                    // TIER 1-3: Обычный, но сочный крит
                    level.sendParticles(ParticleTypes.CRIT, x, y, z, 20, 0.4, 0.4, 0.4, 0.2);
                    level.sendParticles(new DustParticleOptions(0x00FFFF, 1.5f), x, y, z, 10, 0.3, 0.3, 0.3, 0.1); // Голубая пыль скорости
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 2, 0.2, 0.2, 0.2, 0.1);
                    level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                else if (diff >= 4 && diff <= 6) {
                    // TIER 4-6: Огненно-электрический взрыв
                    level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.5, 0.5, 0.5, 0.2);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 25, 0.4, 0.4, 0.4, 0.15);
                    level.sendParticles(new DustParticleOptions(0xFF8C00, 2.0f), x, y, z, 30, 0.5, 0.5, 0.5, 0.15); // Оранжевая пыль
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 4, 0.3, 0.3, 0.3, 0.1);
                    level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.8f);
                    level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.2f);
                }
                else if (diff >= 7 && diff <= 8) {
                    // TIER 7-8: Разрушительный взрыв с молниями и огнем
                    level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 2, 0.1, 0.1, 0.1, 0.0);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 50, 0.6, 0.6, 0.6, 0.2);
                    level.sendParticles(new DustParticleOptions(0xFF0000, 2.5f), x, y, z, 40, 0.6, 0.6, 0.6, 0.2); // Красная пыль
                    level.sendParticles(ParticleTypes.LAVA, x, y, z, 15, 0.4, 0.4, 0.4, 0.1);

                    level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.2f);
                    level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                else if (diff == 9) {
                    // TIER 9: АБСОЛЮТНЫЙ РАЗРЫВ ПРОСТРАНСТВА (Ударная волна)
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 60, 1.0, 1.0, 1.0, 0.3);
                    level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 50, 1.0, 1.0, 1.0, 0.2);
                    level.sendParticles(new DustParticleOptions(0xFFFFFF, 3.0f), x, y, z, 50, 1.0, 1.0, 1.0, 0.3); // Белая вспышка
                    level.sendParticles(new DustParticleOptions(0x00FFFF, 2.5f), x, y, z, 50, 1.0, 1.0, 1.0, 0.3); // Голубая скорость
                    level.sendParticles(new DustParticleOptions(0xFF0000, 2.5f), x, y, z, 50, 1.0, 1.0, 1.0, 0.3); // Красный урон

                    // Эпичное кольцо по земле
                    for (int i = 0; i < 360; i += 10) {
                        double rad = Math.toRadians(i);
                        level.sendParticles(new DustParticleOptions(0x00FFFF, 2.0f),
                                x + Math.cos(rad) * 3.0, target.getY() + 0.2, z + Math.sin(rad) * 3.0, 1, 0.0, 0.1, 0.0, 0.0);
                    }

                    // Оглушительные звуки
                    level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.5f);
                    level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 1.8f);
                    level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.8f);
                }

                attacker.sendOverlayMessage(Component.literal("§bСкоростной крит! §fРазница: " + diff + " ур."));
            }
        }
    }

    private int getSpeedTier(LivingEntity entity) {
        int level = 5;
        if (entity.hasEffect(MobEffects.SPEED)) level += entity.getEffect(MobEffects.SPEED).getAmplifier() + 1;
        if (entity.hasEffect(MobEffects.SLOWNESS)) level -= entity.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1;
        return Math.max(1, Math.min(level, 10));
    }
}