package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;
import java.util.Random;

public class RunnerPassiveAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "runner_passive"; }
    @Override public String getDisplayName() { return "Разгон"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        return "Постоянно даёт Скорость " + toRoman(amp + 1) + ". Шанс 5% нанести крит, зависящий от разницы в скорости.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        return List.of("§7Эффект: §bСкорость " + toRoman(amp + 1) + " §7(постоянно)");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        double chance = ModConfig.getInstance().getEffect("runner_crit_chance", 0.05);
        if (RNG.nextDouble() < chance) {
            double attackerSpeed = attacker.getDeltaMovement().horizontalDistance();
            double targetSpeed = target.getDeltaMovement().horizontalDistance();
            if (attackerSpeed > targetSpeed) {
                double mult = ModConfig.getInstance().getEffect("runner_crit_speed_multiplier", 10.0);
                float bonus = (float) ((attackerSpeed - targetSpeed) * mult);
                if (bonus > 0) {
                    target.hurtServer(attacker.level(), attacker.damageSources().playerAttack(attacker), bonus);

                    ServerLevel level = attacker.level();
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
                }
            }
        }
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.runnerSprintTicks > 0) return;

        int amplifier = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);

        MobEffectInstance current = player.getEffect(MobEffects.SPEED);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != amplifier;
        boolean lowDuration = current != null && current.getDuration() < 40;
        boolean needsRefresh = wasAbsent || wrongAmp || lowDuration;

        if (needsRefresh) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amplifier, true, false));
        }
        if (wasAbsent) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.6f, 1.5f);
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (player.tickCount % 8 == 0) {
            level.sendParticles(new DustParticleOptions(0x4D99FF, 1.0f),
                    x, y + 0.05, z, 3, 0.35, 0.03, 0.35, 0.02);
        }
        if (player.tickCount % 12 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0x80E6FF, 0.9f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        if (player.tickCount % 20 == 0) {
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                double rx = x + Math.cos(angle) * 0.8;
                double rz = z + Math.sin(angle) * 0.8;
                level.sendParticles(new DustParticleOptions(0x4D99FF, 0.8f),
                        rx, y + 0.05, rz, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void applyPassiveSpeed(ServerPlayer player) {
        int amplifier = ModConfig.getInstance().getIntEffect("runner_passive_amplifier", 1);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, amplifier, true, false));
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.SPEED);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}