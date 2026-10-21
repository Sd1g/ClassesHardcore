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

public class StuntmanParkourMasterAbility implements Ability {
    private static final Map<UUID, Integer> LAST_STEPS = new HashMap<>();

    @Override public String getId() { return "parkour_master"; }
    @Override public String getDisplayName() { return "Мастер паркура"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/parkour_master"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int blocks = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        return "За каждые " + blocks + " блоков лазания улучшает Паркур и Сцепление.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int blocks = AbilityClientState.stuntmanClimbedBlocks;
        int perStep = Math.max(1, cfg.getIntEffect("stuntman_master_blocks_per_step", 500));
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);

        int steps = Math.min(blocks / perStep, maxSteps);
        int toNext = perStep - (blocks % perStep);
        if (blocks >= perStep * maxSteps) toNext = 0;

        return List.of(
                "§7Блоков пройдено: §b" + blocks,
                "§7Текущий бонус: §a+" + steps + " сек.",
                (toNext > 0 ? "§7До след. бонуса: §f" + toNext + " бл." : "§7Максимальный бонус")
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int blocksPerStep = Math.max(1, cfg.getIntEffect("stuntman_master_blocks_per_step", 500));
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);

        PlayerClassData data = PlayerClassData.get(player);
        int currentStep = Math.min(data.stuntmanClimbedBlocks / blocksPerStep, maxSteps);

        int lastStep = LAST_STEPS.getOrDefault(player.getUUID(), 0);

        // Если достигнут новый рубеж - триггерим эпичный эффект
        if (currentStep > lastStep) {
            ServerLevel level = player.level();

            // HUD Уведомление
            ServerPlayNetworking.send(player, new HudMessagePayload("§aКаскадёр: Мастерство улучшено!", 0xFF55FF55, 80));

            // Эпичные звуки (Фанфары достижения + Уровень)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);

            // Эпичные эффекты (Зелено-золотая гамма ловкости)
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            // Золотые и зеленые искры
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.5, 1.0, 0.5, 0.2);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 30, 0.6, 0.6, 0.6, 0.1);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.8, 1.2, 0.8, 0.1);

            // Зеленая пыль (цвет ловкости)
            level.sendParticles(new DustParticleOptions(0x00FF00, 2.0f), x, y, z, 40, 0.6, 1.0, 0.6, 0.1);

            // Зеленое кольцо по земле
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f),
                        x + Math.cos(rad) * 2.0, player.getY() + 0.2, z + Math.sin(rad) * 2.0, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        LAST_STEPS.put(player.getUUID(), currentStep);
    }
}