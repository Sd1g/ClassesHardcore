package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class ClimbingAbility implements Ability {
    @Override public String getId() { return "climbing_toggle"; }
    @Override public String getDisplayName() { return "Сцепление"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 100; }
    @Override public int getTickInterval() { return 1; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double speed = cfg.getEffect("climbing_speed", 0.1) * 20;
        int cdSec = cfg.getCooldown("climbing_toggle", getBaseCooldown()) / 20;
        return "Позволяет карабкаться по стенам (~" + fmt(speed) + " блока/сек). Начать можно у стены 2+ блока высотой. " +
                "Переключается повторным нажатием. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        boolean active = false;
        if (player instanceof ServerPlayer sp) active = PlayerClassData.get(sp).climbingActive;
        return List.of("§7Статус: " + (active ? "§aВключено" : "§7Выключено"));
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.climbingActive = !data.climbingActive;
        data.climbingInProgress = false;
        PlayerClassData.markDirty(player);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (data.climbingActive) {
            level.sendParticles(ParticleTypes.CLOUD, x, y + 0.5, z, 15, 0.5, 0.4, 0.5, 0.05);
            level.sendParticles(new DustParticleOptions(0xFFD700, 1.3f), x, y + 0.8, z, 12, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.0f), x, y + 1.2, z, 8, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.3f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);
            player.sendOverlayMessage(Component.literal("§aСцепление включено."));
        } else {
            player.removeEffect(MobEffects.LEVITATION);
            level.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 10, 0.5, 0.4, 0.5, 0.05);
            level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 0.9f);
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
        BlockPos frontBelow = feet.below().relative(facing);

        boolean frontFeetSolid = isSolid(level, frontFeet);
        boolean frontAboveSolid = isSolid(level, frontAbove);
        boolean frontBelowSolid = isSolid(level, frontBelow);

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

            if (player.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
            if (player.tickCount % 10 == 0) {
                level.sendParticles(new DustParticleOptions(0xFFD700, 1.0f), player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.25, 0.2, 0.25, 0.02);
            }
        } else if (frontBelowSolid) {
            data.climbingInProgress = false;
            player.removeEffect(MobEffects.LEVITATION);

            // Уменьшенный прыжок
            double fx = facing.getStepX() * 0.1;
            double fz = facing.getStepZ() * 0.1;
            player.setDeltaMovement(fx, 0.2, fz);
            player.hurtMarked = true;

            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8f, 1.8f);
        } else {
            data.climbingInProgress = false;
            if (player.hasEffect(MobEffects.LEVITATION)) player.removeEffect(MobEffects.LEVITATION);
        }
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }
}