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

public class PorcupineHardeningAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_HITS = new java.util.HashMap<>();

    @Override public String getId() { return "porcupine_hardening"; }
    @Override public String getDisplayName() { return "Закалка шипов"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_hardening"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int hits = cfg.getIntEffect("porcupine_hardening_hits_per_step", 500);
        return "За каждые " + hits + " полученных ударов радиус «Шипов» растет.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int hits = AbilityClientState.porcupineHitsTaken;
        int perStep = Math.max(1, cfg.getIntEffect("porcupine_hardening_hits_per_step", 500));
        double radPerStep = cfg.getEffect("porcupine_hardening_radius_per_step", 0.5);
        double maxSteps = cfg.getEffect("porcupine_hardening_max_steps", 4.0);

        double bonus = Math.min((hits / (double)perStep) * radPerStep, maxSteps * radPerStep);
        int toNext = perStep - (hits % perStep);

        return List.of(
                "§7Получено ударов: §b" + hits,
                "§7Текущий бонус: §a+" + bonus + " блоков",
                "§7До след. бонуса: §f" + toNext + " ударов"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int hits = AbilityClientState.porcupineHitsTaken;
        int perStep = Math.max(1, ModConfig.getInstance().getIntEffect("porcupine_hardening_hits_per_step", 500));

        int currentStep = hits / perStep;
        int lastStep = LAST_HITS.getOrDefault(player.getUUID(), 0);

        if (currentStep > lastStep) {
            ServerLevel level = player.level();
            ServerPlayNetworking.send(player, new HudMessagePayload("§eШипы: Радиус увеличен!", 0xFFFFAA00, 80));

            // ЭПИЧНЫЙ ЗВУК
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 1.5f, 1.0f);

            // ЭПИЧНЫЙ ЭФФЕКТ (Тематика дикобраза: коричневые искры и взрыв шипов)
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(ParticleTypes.CRIT, x, y, z, 100, 1.0, 1.0, 1.0, 0.2);
            level.sendParticles(new DustParticleOptions(0x8B4513, 2.0f), x, y, z, 80, 1.0, 1.0, 1.0, 0.2);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 40, 0.8, 0.8, 0.8, 0.1);

            // Кольцо шипов по земле
            for (int i = 0; i < 360; i += 15) {
                double rad = Math.toRadians(i);
                level.sendParticles(ParticleTypes.CRIT,
                        x + Math.cos(rad) * 2.0, player.getY() + 0.2, z + Math.sin(rad) * 2.0, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        LAST_HITS.put(player.getUUID(), currentStep);
    }
}