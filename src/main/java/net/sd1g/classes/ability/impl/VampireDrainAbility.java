package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.VampireSyncPayload;

import java.util.List;

public class VampireDrainAbility implements Ability {
    @Override public String getId() { return "vampire_drain"; }
    @Override public String getDisplayName() { return "Вампиризм"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("vampire_drain", 1200);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("vampire_active_duration_ticks", 200) / 20;
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.70) * 100);
        int cd = cfg.getCooldown("vampire_drain", 1200) / 20;
        return "На " + dur + " сек удары лечат на " + percent + "% урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int percent = (int) Math.round(ModConfig.getInstance().getEffect("vampire_heal_percent", 0.70) * 100);
        return List.of("§7Лечение: §a" + percent + "% §7от урона");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.vampireActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        int duration = ModConfig.getInstance().getIntEffect("vampire_active_duration_ticks", 200);
        PlayerClassData data = PlayerClassData.get(player);
        data.vampireActiveTicks = duration;
        PlayerClassData.markDirty(player);
        ServerPlayNetworking.send(player, new VampireSyncPayload(duration));
        player.sendOverlayMessage(Component.literal("§cВампиризм активирован!"));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks <= 0) return;
        data.vampireActiveTicks--;
        if (data.vampireActiveTicks <= 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));
            player.sendOverlayMessage(Component.literal("§cВампиризм закончился"));
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks <= 0) return;

        double percent = ModConfig.getInstance().getEffect("vampire_heal_percent", 0.70);
        attacker.heal((float) (damageDealt * percent));
    }
}