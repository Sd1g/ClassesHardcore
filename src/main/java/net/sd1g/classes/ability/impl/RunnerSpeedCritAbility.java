package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class RunnerSpeedCritAbility implements Ability {
    @Override public String getId() { return "runner_speed_crit"; }
    @Override public String getDisplayName() { return "Скоростной крит"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

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

                attacker.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
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