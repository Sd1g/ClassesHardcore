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

import java.util.List;
import java.util.Random;

public class GluttonyCravingsAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "cravings"; }
    @Override public String getDisplayName() { return "Чревоугодие"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("cravings_chance", 0.40) * 100);
        int amp = cfg.getIntEffect("cravings_hunger_amplifier", 2) + 1;
        int dur = cfg.getIntEffect("cravings_hunger_duration_ticks", 140) / 20;
        return "С шансом " + chance + "% при ударе накладывает Голод " + toRoman(amp) + " на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("cravings_chance", 0.40) * 100);
        int amp = cfg.getIntEffect("cravings_hunger_amplifier", 2) + 1;
        int dur = cfg.getIntEffect("cravings_hunger_duration_ticks", 140);
        return List.of(
                "§7Шанс: §a" + chance + "%",
                "§7Эффект: §6Голод " + toRoman(amp) + " §7на §f" + (dur / 20) + " сек."
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("cravings_chance", 0.40);
        if (RNG.nextDouble() >= chance) return;

        int amp = cfg.getIntEffect("cravings_hunger_amplifier", 2);
        int dur = cfg.getIntEffect("cravings_hunger_duration_ticks", 140);

        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, amp, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        // Частицы срабатывания — бурые/золотые искры
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 8, 0.4, 0.4, 0.4, 0.04);
        level.sendParticles(new DustParticleOptions(0xAA4400, 1.4f), x, y, z, 12, 0.4, 0.4, 0.4, 0.1);
        level.sendParticles(new DustParticleOptions(0xD9A400, 1.1f), x, y, z, 8, 0.4, 0.4, 0.4, 0.08);

        level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER,
                SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}