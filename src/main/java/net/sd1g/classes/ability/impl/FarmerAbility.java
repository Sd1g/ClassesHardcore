package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HudMessagePayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FarmerAbility implements Ability {
    private static final Map<UUID, Integer> LAST_STEPS = new HashMap<>();

    @Override public String getId() { return "farmer_passive"; }
    @Override public String getDisplayName() { return "Мастерство мотыги"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/farmer_passive"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hoe = (int) Math.round((cfg.getEffect("farmer_hoe_damage_multiplier", 2.0) - 1.0) * 100);
        int other = (int) Math.round((1.0 - cfg.getEffect("farmer_other_damage_multiplier", 0.2)) * 100);
        return "Урон мотыгой +" + hoe + "%. Другое оружие -" + other + "%. Растет от убийств.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int mobsPerStep = Math.max(1, cfg.getIntEffect("farmer_mobs_per_bonus_point", 100));
        // ИСПРАВЛЕНО: Убрано деление на 2.0
        double bonusPerMobStep = cfg.getEffect("farmer_bonus_per_mob_step", 0.5);
        double bonusPerPlayer = cfg.getEffect("farmer_bonus_per_player_kill", 10.0);

        int mobs = AbilityClientState.farmerMobKills;
        int players = AbilityClientState.farmerPlayerKills;

        int step = mobs / mobsPerStep;
        double accumulatedBonus = (step * bonusPerMobStep) + (players * bonusPerPlayer);
        int toNext = mobsPerStep - (mobs % mobsPerStep);

        return List.of(
                "§7Убийств мобов: §b" + mobs,
                "§7Убийств игроков: §c" + players,
                "§7Бонус за " + mobsPerStep + " мобов: §a+" + bonusPerMobStep + " HP",
                "§7Бонус за игрока: §a+" + bonusPerPlayer + " HP",
                "§7Текущий бонус: §c+" + accumulatedBonus + " HP",
                "§7До след. шага: §f" + toNext + " мобов"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);

        int mobsPerStep = Math.max(1, cfg.getIntEffect("farmer_mobs_per_bonus_point", 100));
        int currentStep = data.farmerMobKills / mobsPerStep;

        int lastStep = LAST_STEPS.getOrDefault(player.getUUID(), 0);

        if (currentStep > lastStep) {
            ServerLevel level = player.level();

            ServerPlayNetworking.send(player, new HudMessagePayload("§2Фермер: Мастерство выросло!", 0xFF228B22, 80));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6f, 1.5f);

            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(new DustParticleOptions(0x00FF00, 2.5f), x, y, z, 60, 0.8, 0.8, 0.8, 0.15);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 40, 0.6, 1.0, 0.6, 0.1);

            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 20, 0.8, 0.8, 0.8, 0.1);
            level.sendParticles(ParticleTypes.WITCH, x, y, z, 30, 0.6, 1.0, 0.6, 0.1);

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f),
                        x + Math.cos(rad) * 2.5, player.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        LAST_STEPS.put(player.getUUID(), currentStep);
    }
}