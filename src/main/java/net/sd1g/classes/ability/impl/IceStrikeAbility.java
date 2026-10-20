package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class IceStrikeAbility implements Ability {
    @Override public String getId() { return "ice_strike"; }
    @Override public String getDisplayName() { return "Ледяной удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_strike"); }

    static int computeSlownessAmplifier(Player player) {
        int level = (player != null) ? player.experienceLevel : 0;
        int bonus = level / 50;
        return Math.min(bonus, 2); // Максимум Slowness III (amp 2)
    }

    @Override
    public String getDescription() {
        return "Удар добавляет 0,75 НР чистого урона (не чаще раза в секунду) и накладывает Замедление I на 5 секунд. Каждые 50 уровней опыта усиливают замедление на 1 уровень, максимум III против игроков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int amp = computeSlownessAmplifier(player);
        int level = player.experienceLevel;
        int toNext = 50 - (level % 50);

        return List.of(
                "§7Чистый урон: §c+0.75 НР",
                "§7Замедление: §b" + toRoman(amp + 1) + " §7на §f5 сек.",
                "§7Текущий уровень: §f" + level,
                (amp < 2 ? "§7До след. усиления: §f" + toNext + " ур." : "§7Максимальное замедление")
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (player.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.05, z, 3, 0.4, 0.05, 0.4, 0.01);
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();

        int duration = 100; // 5 секунд
        int amplifier = computeSlownessAmplifier(attacker);

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier, false, true));

        // Чистый урон (0.75 HP = 1.5 урона) не чаще раза в секунду
        if (!data.isOnCooldown("ice_strike_cd", now)) {
            PureDamage.apply(attacker, target, 1.5f);
            data.setCooldown("ice_strike_cd", now, 20); // 1 секунда

            ServerLevel level = attacker.level();
            double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.5, z = target.getZ();
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 12, 0.5, 0.5, 0.5, 0.08);
            level.sendParticles(new DustParticleOptions(0x80E6FF, 1.3f), x, y, z, 10, 0.5, 0.5, 0.5, 0.08);
            level.playSound(null, x, y, z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}