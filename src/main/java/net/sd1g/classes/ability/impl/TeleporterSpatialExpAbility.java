package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
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

import java.util.List;

public class TeleporterSpatialExpAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_BONUS = new java.util.HashMap<>();

    @Override public String getId() { return "spatial_exp"; }
    @Override public String getDisplayName() { return "Пространственный опыт"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/spatial_exp"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int tpPerBonus = Math.max(1, cfg.getIntEffect("teleporter_tp_per_bonus", 200));
        double maxBonus = cfg.getEffect("teleporter_max_bonus", 10.0);
        return "За каждые " + tpPerBonus + " успешных телепортаций макс. дистанция увеличивается на 1 блок (макс. +" + (int)maxBonus + ").";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int tpPerBonus = Math.max(1, cfg.getIntEffect("teleporter_tp_per_bonus", 200));
        double maxBonus = cfg.getEffect("teleporter_max_bonus", 10.0);

        int count = AbilityClientState.teleporterCount;
        int bonus = (int) Math.min(count / (double)tpPerBonus, maxBonus);
        int toNext = tpPerBonus - (count % tpPerBonus);

        return List.of(
                "§7Успешных ТП: §b" + count,
                "§7Текущий бонус: §b+" + bonus + " блоков",
                "§7До след. бонуса: §f" + toNext + " ТП"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        ModConfig cfg = ModConfig.getInstance();
        int tpPerBonus = Math.max(1, cfg.getIntEffect("teleporter_tp_per_bonus", 200));
        int maxBonus = cfg.getIntEffect("teleporter_max_bonus", 10);

        int currentBonus = Math.min(data.teleporterCount / tpPerBonus, maxBonus);
        int last = LAST_BONUS.getOrDefault(player.getUUID(), 0);

        if (currentBonus > last) {
            ServerLevel level = player.level();
            ServerPlayNetworking.send(player, new HudMessagePayload("§dТелепорт: Дальность увеличена!", 0xFF55FFFF, 80));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);

            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            // Эпичный фиолетовый взрыв
            level.sendParticles(new DustParticleOptions(0xB34DFF, 2.5f), x, y, z, 100, 1.0, 2.0, 1.0, 0.3);
            level.sendParticles(new DustParticleOptions(0x8B008B, 3.0f), x, y, z, 40, 1.0, 1.5, 1.0, 0.1);
            level.sendParticles(new DustParticleOptions(0x4B0082, 1.5f), x, y, z, 50, 1.2, 1.2, 1.2, 0.1);
            level.sendParticles(new DustParticleOptions(0xFFD700, 1.2f), x, y, z, 30, 0.8, 1.0, 0.8, 0.1);

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0xB34DFF, 2.0f),
                        x + Math.cos(rad) * 2.5, player.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        LAST_BONUS.put(player.getUUID(), currentBonus);
    }
}