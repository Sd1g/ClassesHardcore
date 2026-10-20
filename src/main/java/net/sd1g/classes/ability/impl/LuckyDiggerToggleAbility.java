package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("digger_toggle_cooldown", 600);
    }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("digger_toggle_cooldown", 600) / 20;
        return "Переключает бонус добычи между Ускорением копания и Удачей. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        String mode = AbilityClientState.luckyDiggerMode;
        return List.of("§7Текущий режим: " + ("efficiency".equals(mode) ? "§bЭффективность" : "§aУдача"));
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.luckyDiggerMode = "efficiency".equals(data.luckyDiggerMode) ? "luck" : "efficiency";
        PlayerClassData.markDirty(player);

        LuckyDiggerMiningAbility.applyCurrentEffect(player);
        ServerPlayNetworking.send(player, new LuckyDiggerSyncPayload(data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
        player.sendOverlayMessage(Component.literal("§aРежим: " + ("luck".equals(data.luckyDiggerMode) ? "Удача" : "Эффективность")));
        return true;
    }
}