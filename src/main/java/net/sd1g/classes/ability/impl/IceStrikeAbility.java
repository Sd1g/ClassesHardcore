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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class IceStrikeAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("ice_warrior_bonus_damage");

    @Override public String getId() { return "ice_strike"; }
    @Override public String getDisplayName() { return "Ледяной удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    static int computeSlownessAmplifier(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int baseAmp = cfg.getIntEffect("ice_strike_slowness_amplifier", 0);
        int levelsPerStep = cfg.getIntEffect("ice_strike_levels_per_amplifier_step", 30);
        int maxAmp = cfg.getIntEffect("ice_strike_max_amplifier", 4);
        int level = (player != null) ? player.experienceLevel : 0;
        int bonus = levelsPerStep > 0 ? level / levelsPerStep : 0;
        return Math.min(baseAmp + bonus, maxAmp);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int bonus = cfg.getIntEffect("ice_strike_bonus_damage", 1);
        int dur = cfg.getIntEffect("ice_strike_slowness_duration_ticks", 100) / 20;
        int perStep = cfg.getIntEffect("ice_strike_levels_per_amplifier_step", 30);
        return "Каждый удар +" + bonus + " урона и Замедление на " + dur + " сек. "
                + "Каждые " + perStep + " уровней — +1 уровень Замедления.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int bonus = cfg.getIntEffect("ice_strike_bonus_damage", 1);
        int dur = cfg.getIntEffect("ice_strike_slowness_duration_ticks", 100);
        int amp = computeSlownessAmplifier(player);
        return List.of(
                "§7Бонус к урону: §c+" + bonus,
                "§7Замедление: §b" + toRoman(amp + 1) + " §7на §f" + (dur / 20) + " сек."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int bonus = ModConfig.getInstance().getIntEffect("ice_strike_bonus_damage", 1);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            AttributeModifier existing = attr.getModifier(MODIFIER_ID);
            if (existing == null || (int) existing.amount() != bonus) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER_ID, (double) bonus, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        // Аура льда — снежинки у ног + столб
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (player.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    x, y + 0.05, z, 3, 0.4, 0.05, 0.4, 0.01);
        }
        if (player.tickCount % 12 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0x80E6FF, 0.8f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        int duration = cfg.getIntEffect("ice_strike_slowness_duration_ticks", 100);
        int amplifier = computeSlownessAmplifier(attacker);

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, duration, amplifier, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        // Частицы на цель — снежинки + лёд
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 12, 0.5, 0.5, 0.5, 0.08);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 6, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(new DustParticleOptions(0x80E6FF, 1.3f), x, y, z, 10, 0.5, 0.5, 0.5, 0.08);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.0f), x, y, z, 6, 0.4, 0.4, 0.4, 0.05);

        level.playSound(null, x, y, z, SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    public static void removeBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}