package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;

import java.util.List;

public class FarmerAbility implements Ability {
    @Override public String getId() { return "hoe_mastery"; }
    @Override public String getDisplayName() { return "Мастерство мотыги"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hoeBonus = (int) Math.round((cfg.getEffect("farmer_hoe_damage_multiplier", 2.0) - 1.0) * 100);
        int otherPenalty = (int) Math.round((1.0 - cfg.getEffect("farmer_other_damage_multiplier", 0.2)) * 100);
        int mobsPerStep = cfg.getIntEffect("farmer_mobs_per_bonus_point", 100);
        return "Урон мотыгой +" + hoeBonus + "%. За каждые " + mobsPerStep
                + " убийств мобов — бонус. Другое оружие −" + otherPenalty + "%.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double hoeMult = cfg.getEffect("farmer_hoe_damage_multiplier", 2.0);
        double otherMult = cfg.getEffect("farmer_other_damage_multiplier", 0.2);
        int mobsPerStep = cfg.getIntEffect("farmer_mobs_per_bonus_point", 100);
        double bonusPerStep = cfg.getEffect("farmer_bonus_per_mob_step", 0.25);
        double bonusPerPlayer = cfg.getEffect("farmer_bonus_per_player_kill", 5.0);

        int mobKills = AbilityClientState.farmerMobKills;
        int playerKills = AbilityClientState.farmerPlayerKills;

        int step = mobsPerStep > 0 ? mobKills / mobsPerStep : 0;
        double accumulatedBonus = step * bonusPerStep + playerKills * bonusPerPlayer;

        int toNext = mobsPerStep > 0 ? mobsPerStep - (mobKills % mobsPerStep) : 0;
        if (mobsPerStep > 0 && mobKills % mobsPerStep == 0) toNext = mobsPerStep;

        return List.of(
                "§7Мотыга: §a×" + fmt(hoeMult) + " §7урона",
                "§7Другое оружие: §c×" + fmt(otherMult) + " §7урона",
                "§7Убийств мобов: §f" + mobKills,
                "§7Бонус за " + mobsPerStep + " мобов: §c+" + fmt(bonusPerStep),
                "§7До следующего бонуса: §f" + toNext + " мобов",
                "§7Убийств игроков: §f" + playerKills + " §7(по §c+" + fmt(bonusPerPlayer) + "§7)",
                "§7Накоплено бонуса: §c+" + fmt(accumulatedBonus)
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Зелёная пыль у ног
        if (player.tickCount % 12 == 0) {
            level.sendParticles(new DustParticleOptions(0x33FF33, 0.9f),
                    x, y + 0.05, z, 3, 0.4, 0.03, 0.4, 0.02);
        }
        // Столб зелёных частиц
        if (player.tickCount % 20 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.3 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0x33FF33, 0.7f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        // Редкие HAPPY_VILLAGER
        if (player.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.8, z, 2, 0.4, 0.3, 0.4, 0.03);
        }
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.2f", v);
    }
}