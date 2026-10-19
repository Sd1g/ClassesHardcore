package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class HealAbility implements Ability {
    @Override public String getId() { return "heal"; }
    @Override public String getDisplayName() { return "Исцеление"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 440; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amount = (int) cfg.getEffect("heal_amount", 2.0);
        int regenTicks = cfg.getIntEffect("heal_regen_ticks", 100);
        int regenAmp = cfg.getIntEffect("heal_regen_amplifier", 0);
        return "Мгновенно восстанавливает " + amount + " HP и накладывает Регенерацию "
                + toRoman(regenAmp + 1) + " на " + (regenTicks / 20) + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int amount = (int) cfg.getEffect("heal_amount", 2.0);
        int regenTicks = cfg.getIntEffect("heal_regen_ticks", 100);
        int regenAmp = cfg.getIntEffect("heal_regen_amplifier", 0);
        int cd = cfg.getCooldown("heal", getBaseCooldown());
        return List.of(
                "§7Мгновенное лечение: §a" + amount + " HP",
                "§7Регенерация: §b" + toRoman(regenAmp + 1) + " §7на §f" + (regenTicks / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double amount = cfg.getEffect("heal_amount", 2.0);
        int regenTicks = cfg.getIntEffect("heal_regen_ticks", 100);
        int regenAmp = cfg.getIntEffect("heal_regen_amplifier", 0);

        player.heal((float) amount);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // ---- Активация: восходящий золотой столб ----
        for (int i = 0; i < 12; i++) {
            level.sendParticles(ParticleTypes.HEART,
                    x, y + 0.4 + i * 0.15, z, 1, 0.3, 0, 0.3, 0.0);
        }
        level.sendParticles(new DustParticleOptions(0xFFAA00, 1.4f),
                x, y + 0.5, z, 18, 0.5, 0.4, 0.5, 0.03);
        level.sendParticles(new DustParticleOptions(0xFFD580, 1.0f),
                x, y + 1.0, z, 12, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.END_ROD,
                x, y + 1.2, z, 8, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y + 1.0, z, 6, 0.4, 0.4, 0.4, 0.03);

        // Два звука: торжественный + звонкий акцент
        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.1f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.9f, 1.8f);

        player.sendOverlayMessage(Component.literal(
                "§aИсцеление. §f+" + (int) amount + " HP."));
        return true;
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}