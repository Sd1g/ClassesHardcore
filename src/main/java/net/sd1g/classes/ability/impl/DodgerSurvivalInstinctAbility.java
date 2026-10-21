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
import net.sd1g.classes.network.HudMessagePayload;

import java.util.List;

public class DodgerSurvivalInstinctAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_STEPS = new java.util.HashMap<>();

    @Override public String getId() { return "dodger_survival_instinct"; }
    @Override public String getDisplayName() { return "Инстинкт выживания"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/dodger_survival_instinct"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dodgesPerStep = cfg.getIntEffect("dodger_kills_per_step", 300);
        double maxBonus = cfg.getEffect("dodger_max_bonus_chance", 0.10) * 100;
        return "За каждые " + dodgesPerStep + " уклонений шанс уклонения растет на 1% (макс. +" + (int)maxBonus + "%).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dodges = AbilityClientState.dodgerDodges;
        int perStep = Math.max(1, cfg.getIntEffect("dodger_kills_per_step", 300));
        double maxBonus = cfg.getEffect("dodger_max_bonus_chance", 0.10) * 100;

        int currentBonus = (int) Math.min((dodges / (double)perStep), maxBonus);
        int toNext = perStep - (dodges % perStep);

        return List.of(
                "§7Уклонений: §b" + dodges,
                "§7Текущий бонус: §a+" + currentBonus + "%",
                (currentBonus < maxBonus ? "§7До след. бонуса: §f" + toNext + " уклонений" : "§7Максимальный бонус")
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int dodges = AbilityClientState.dodgerDodges;
        int perStep = Math.max(1, cfg.getIntEffect("dodger_kills_per_step", 300));

        int currentStep = dodges / perStep;
        int lastStep = LAST_STEPS.getOrDefault(player.getUUID(), 0);

        if (currentStep > lastStep) {
            ServerLevel level = player.level();

            ServerPlayNetworking.send(player, new HudMessagePayload("§aИнстинкт: Уклонение +1%!", 0xFF55FF55, 80));

            // ЭПИЧНЫЕ ЗВУКИ
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0f, 1.5f);

            // ЭПИЧНЫЕ ЭФФЕКТЫ (Зеленая гамма ловкости)
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.5, 1.0, 0.5, 0.2);
            level.sendParticles(new DustParticleOptions(0x00FF00, 2.5f), x, y, z, 60, 1.0, 1.0, 1.0, 0.15);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 30, 1.0, 0.5, 1.0, 0.1);

            // Зеленое кольцо по земле
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0x00FF00, 2.0f),
                        x + Math.cos(rad) * 2.5, player.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        LAST_STEPS.put(player.getUUID(), currentStep);
    }
}