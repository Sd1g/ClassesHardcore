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
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.LuckyDiggerSyncPayload;

import java.util.List;

public class LuckyDiggerToggleAbility implements Ability {
    @Override public String getId() { return "toggle_bonus"; }
    @Override public String getDisplayName() { return "Смена бонуса"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; } // 30 секунд

    @Override
    public String getDescription() {
        return "Переключает бонус добычи между Ускорением копания и Удачей. Перезарядка: 30 сек.";
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
        double x = player.getX(), y = player.getY(), z = player.getZ();
        boolean luckMode = "luck".equals(data.luckyDiggerMode);
        int color = luckMode ? 0x33FF4D : 0x3399FF;

        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.3f, luckMode ? 1.5f : 1.0f);
        level.sendParticles(new DustParticleOptions(color, 1.4f), x, y + 0.5, z, 20, 0.6, 0.5, 0.6, 0.05);

        player.sendOverlayMessage(Component.literal("§aСмена бонуса. §fРежим: " + (luckMode ? "Удача" : "Эффективность") + "."));
        return true;
    }
}