package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.CooldownSyncPayload;

import java.util.List;

public class ClimbingAbility implements Ability {
    @Override public String getId() { return "climbing_toggle"; }
    @Override public String getDisplayName() { return "Сцепление"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("climbing_toggle", 100);
    }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("climbing_toggle", 100) / 20;
        return "Включает/выключает возможность карабкаться по стенам. Базовая перезарядка: " + cd + " сек. (растет от прогрессии)";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        boolean active = false;
        if (player instanceof ServerPlayer sp) active = PlayerClassData.get(sp).climbingActive;
        return List.of("§7Статус: " + (active ? "§aВключено" : "§7Выключено"));
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int blocksPerStep = cfg.getIntEffect("stuntman_master_blocks_per_step", 500);
        int maxSteps = cfg.getIntEffect("stuntman_master_max_steps", 5);
        int bonusCooldown = cfg.getIntEffect("stuntman_master_cooldown_reduction", 20);

        int extraCd = Math.min((data.stuntmanClimbedBlocks / blocksPerStep) * bonusCooldown, maxSteps * bonusCooldown);
        return getBaseCooldown() + extraCd;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.climbingActive = !data.climbingActive;
        data.climbingInProgress = false;
        PlayerClassData.markDirty(player);

        if (data.climbingActive) {
            player.sendOverlayMessage(Component.literal("§aСцепление включено."));
        } else {
            player.removeEffect(MobEffects.LEVITATION);
            player.sendOverlayMessage(Component.literal("§7Сцепление выключено."));
        }
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (!data.climbingActive) {
            if (player.hasEffect(MobEffects.LEVITATION)) player.removeEffect(MobEffects.LEVITATION);
            data.climbingInProgress = false;
            return;
        }

        ServerLevel level = player.level();
        Direction facing = player.getDirection();
        BlockPos feet = player.blockPosition();
        BlockPos frontFeet = feet.relative(facing);
        BlockPos frontAbove = feet.above().relative(facing);

        boolean frontFeetSolid = isSolid(level, frontFeet);
        boolean frontAboveSolid = isSolid(level, frontAbove);

        if (!data.climbingInProgress) {
            if (player.onGround() && frontFeetSolid && frontAboveSolid) {
                data.climbingInProgress = true;
            } else {
                if (player.hasEffect(MobEffects.LEVITATION)) player.removeEffect(MobEffects.LEVITATION);
                return;
            }
        }

        if (frontFeetSolid) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 5, 1, true, false, false));
            player.resetFallDistance();

            double dy = player.getDeltaMovement().y;
            if (dy > 0) {
                data.stuntmanClimbAccumulator += dy;
                if (data.stuntmanClimbAccumulator >= 1.0) {
                    int blocks = (int) data.stuntmanClimbAccumulator;
                    data.stuntmanClimbedBlocks += blocks;
                    data.stuntmanClimbAccumulator -= blocks;
                    PlayerClassData.markDirty(player);
                    ServerPlayNetworking.send(player, new CooldownSyncPayload("stuntman_blocks", data.stuntmanClimbedBlocks));
                }
            }
        } else {
            data.climbingInProgress = false;
            if (player.hasEffect(MobEffects.LEVITATION)) player.removeEffect(MobEffects.LEVITATION);
        }
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }
}