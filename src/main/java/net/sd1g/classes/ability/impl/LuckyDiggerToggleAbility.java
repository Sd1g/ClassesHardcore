package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.LuckyDiggerSyncPayload;

import java.util.List;

public class LuckyDiggerToggleAbility implements Ability {
    @Override public String getId() { return "toggle_bonus"; }
    @Override public String getDisplayName() { return "Смена бонуса"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("toggle_bonus", getBaseCooldown());
        return "Переключает бонус между Эффективностью и Удачей. Кулдаун " + (cd / 20) + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        String mode = AbilityClientState.luckyDiggerMode;
        String modeName = "efficiency".equals(mode) ? "§bЭффективность" : "§aУдача";
        String nextName = "efficiency".equals(mode) ? "§aУдача" : "§bЭффективность";
        return List.of(
                "§7Текущий режим: " + modeName,
                "§7При активации: " + nextName
        );
    }

    @Override
    public boolean isHighlighted(Player player) {
        return "luck".equals(AbilityClientState.luckyDiggerMode);
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.luckyDiggerMode = "efficiency".equals(data.luckyDiggerMode) ? "luck" : "efficiency";
        PlayerClassData.markDirty(player);

        LuckyDiggerMiningAbility.applyCurrentEffect(player);

        ServerPlayNetworking.send(player, new LuckyDiggerSyncPayload(
                data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        boolean luckMode = "luck".equals(data.luckyDiggerMode);

        // Кольцо вокруг игрока
        int points = 20;
        int color = luckMode ? 0x33FF4D : 0x3399FF;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            level.sendParticles(new DustParticleOptions(color, 1.3f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        // Восходящий столб
        for (int i = 0; i < 6; i++) {
            level.sendParticles(new DustParticleOptions(color, 1.1f),
                    x, y + 0.3 + i * 0.25, z, 1, 0.15, 0, 0.15, 0);
        }

        if (luckMode) {
            // Удача — зелёные + золотые
            level.sendParticles(new DustParticleOptions(0x33FF4D, 1.4f), x, y + 0.5, z, 20, 0.6, 0.5, 0.6, 0.05);
            level.sendParticles(new DustParticleOptions(0xFFE61A, 1.2f), x, y + 1.0, z, 15, 0.5, 0.5, 0.5, 0.05);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 15, 0.6, 0.6, 0.6, 0.06);
            level.sendParticles(ParticleTypes.END_ROD, x, y + 1.2, z, 8, 0.4, 0.4, 0.4, 0.02);

            level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.3f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.5f, 1.8f);

            player.sendOverlayMessage(Component.literal("§aСмена бонуса. §fРежим: Удача."));
        } else {
            // Эффективность — синие + электрические
            level.sendParticles(new DustParticleOptions(0x3399FF, 1.4f), x, y + 0.5, z, 20, 0.6, 0.5, 0.6, 0.05);
            level.sendParticles(new DustParticleOptions(0x80E6FF, 1.2f), x, y + 1.0, z, 15, 0.5, 0.5, 0.5, 0.05);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.0, z, 15, 0.5, 0.5, 0.5, 0.05);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.2, z, 10, 0.6, 0.4, 0.6, 0.3);

            level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.3f, 1.0f);
            level.playSound(null, x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 1.2f, 1.4f);

            player.sendOverlayMessage(Component.literal("§bСмена бонуса. §fРежим: Эффективность."));
        }
        return true;
    }
}