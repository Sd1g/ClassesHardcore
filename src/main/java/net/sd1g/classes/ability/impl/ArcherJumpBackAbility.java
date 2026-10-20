package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class ArcherJumpBackAbility implements Ability {
    @Override public String getId() { return "archer_jump_back"; }
    @Override public String getDisplayName() { return "Разрыв дистанции"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("archer_jump_back", 700);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int cd = cfg.getCooldown("archer_jump_back", 700) / 20;
        return "Отпрыгивает назад на " + (int)cfg.getEffect("archer_jump_back_distance", 12.0) + " блоков и дает щит. Базовая перезарядка: " + cd + " сек. (уменьшается от убийств)";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Сокращается от убийств из лука");
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int perStep = cfg.getIntEffect("archer_hunter_kills_per_step", 100);
        int reductionTicks = cfg.getIntEffect("archer_hunter_max_reduction_ticks", 200);

        int reduction = Math.min((data.archerBowKills / perStep) * 20, reductionTicks);
        return getBaseCooldown() - reduction;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double dist = cfg.getEffect("archer_jump_back_distance", 12.0);
        int hp = cfg.getIntEffect("archer_jump_back_absorption_hp", 4);
        int dur = cfg.getIntEffect("archer_jump_back_absorption_duration_ticks", 1200);

        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize();

        player.setDeltaMovement(back.x * (dist/5.0), 0.45, back.z * (dist/5.0));
        player.hurtMarked = true;

        player.removeEffect(MobEffects.ABSORPTION);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, Math.max(0, (hp/4) - 1), false, true, true));
        player.setAbsorptionAmount(hp);

        player.sendOverlayMessage(Component.literal("§eРазрыв дистанции."));
        return true;
    }
}