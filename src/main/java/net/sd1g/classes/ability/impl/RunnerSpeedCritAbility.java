package net.sd1g.classes.ability.impl;

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
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class RunnerSpeedCritAbility implements Ability {
    @Override public String getId() { return "runner_speed_crit"; }
    @Override public String getDisplayName() { return "Скоростной крит"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/runner_speed_crit"); }

    @Override
    public String getDescription() {
        return "При ударе с вероятностью 5% наносит дополнительный урон в зависимости от разницы скорости передвижения с противником.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of(
                "§7Шанс: §a5%",
                "§7Работает от разницы эффектов Скорости/Замедления"
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (attacker.getRandom().nextDouble() >= 0.05) return;

        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();

        // Защита от бесконечного цикла урона
        if (data.isOnCooldown("runner_crit_cd", now)) return;

        int attackerTier = getSpeedTier(attacker);
        int targetTier = getSpeedTier(target);
        int diff = attackerTier - targetTier;

        if (diff >= 1) {
            double multiplier = 0;
            float pureDamage = 0;

            // Таблица урона по разнице уровней
            switch (diff) {
                case 1 -> { multiplier = 1.0; pureDamage = 1.5f; }  // +100%, 0.75 HP
                case 2 -> { multiplier = 1.2; pureDamage = 2.5f; }  // +120%, 1.25 HP
                case 3 -> { multiplier = 1.4; pureDamage = 3.0f; }  // +140%, 1.5 HP
                case 4 -> { multiplier = 1.6; pureDamage = 3.5f; }  // +160%, 1.75 HP
                case 5 -> { multiplier = 1.8; pureDamage = 4.0f; }  // +180%, 2 HP
                case 6 -> { multiplier = 2.0; pureDamage = 4.5f; }  // +200%, 2.25 HP
                case 7 -> { multiplier = 2.2; pureDamage = 4.5f; }  // +220%, 2.25 HP
                case 8 -> { multiplier = 2.4; pureDamage = 5.0f; }  // +240%, 2.5 HP
                case 9 -> { multiplier = 3.0; pureDamage = 10.0f; } // +300%, 5 HP
            }

            if (multiplier > 0) {
                // Ставим КД сразу, чтобы избежать рекурсии
                data.setCooldown("runner_crit_cd", now, 10);

                // 1. Наносим бонусный физический урон (множитель)
                // (1 HP = 2 ед. урона, поэтому pureDamage уже в единицах урона)
                if (multiplier > 0) {
                    target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().playerAttack(attacker), damageDealt * (float)multiplier);
                }

                // 2. Наносим чистый урон
                PureDamage.apply(attacker, target, pureDamage);

                // 3. Визуал и сообщение
                ServerLevel level = attacker.level();
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0.2, 0.2, 0.2, 0);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.5f);

                attacker.sendOverlayMessage(Component.literal(
                        "§bСкоростной крит! §fРазница: " + diff + " ур. §7(+" + (int)(multiplier * 100) + "%, " + (pureDamage/2) + " HP)"
                ));
            }
        }
    }

    // Вычисляет уровень скорости от 1 до 10 (где 5 - нейтрально)
    private int getSpeedTier(LivingEntity entity) {
        int level = 5; // Нейтральный уровень (V)
        if (entity.hasEffect(MobEffects.SPEED)) {
            level += entity.getEffect(MobEffects.SPEED).getAmplifier() + 1;
        }
        if (entity.hasEffect(MobEffects.SLOWNESS)) {
            level -= entity.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1;
        }
        return Math.max(1, Math.min(level, 10)); // Ограничиваем от I до X
    }
}