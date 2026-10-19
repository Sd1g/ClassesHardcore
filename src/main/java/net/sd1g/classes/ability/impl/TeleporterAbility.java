package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;
import java.util.Random;

public class TeleporterAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "teleport"; }
    @Override public String getDisplayName() { return "Телепортация"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 100; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double maxDist = cfg.getEffect("teleport_max_distance", 30.0);
        double minDist = cfg.getEffect("teleport_min_distance", 3.0);
        double chance = cfg.getEffect("teleport_success_chance", 0.30);
        return "Мгновенно переносит на " + (int) minDist + "–" + (int) maxDist
                + " блоков вперёд. Шанс успеха: " + (int)(chance*100) + "%. КД зависит от дистанции.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double maxDist = cfg.getEffect("teleport_max_distance", 30.0);
        double minDist = cfg.getEffect("teleport_min_distance", 3.0);
        return List.of(
                "§7Максимальная дальность: §f" + (int) maxDist + " блоков",
                "§7Минимальная дальность: §f" + (int) minDist + " блоков",
                "§7КД: §f2 сек за 1 блок"
        );
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        double distance = computeDistance(player);
        int perBlock = ModConfig.getInstance().getIntEffect("teleport_cooldown_per_block", 40);
        return (int) Math.max(40, distance * perBlock);
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double successChance = cfg.getEffect("teleport_success_chance", 0.30);
        if (RNG.nextDouble() > successChance) {
            player.sendSystemMessage(Component.literal("§cТелепортация не удалась."));
            return true; // Идет в кулдаун
        }

        Vec3 horizontalDir = getHorizontalDirection(player);
        Vec3 eye = player.getEyePosition();
        double distance = computeDistance(player);
        Vec3 rawTarget = eye.add(horizontalDir.scale(distance));

        ServerLevel level = player.level();
        BlockPos standBp = findTargetPosition(level, rawTarget);
        if (standBp == null) {
            player.sendSystemMessage(Component.literal("§cЗдесь телепортироваться нельзя."));
            return true; // Идет в кулдаун
        }

        Vec3 target = new Vec3(rawTarget.x, standBp.getY(), rawTarget.z);

        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 60, 0.6, 1.0, 0.6, 0.15);
        level.sendParticles(ParticleTypes.PORTAL, px, py, pz, 40, 0.6, 1.0, 0.6, 0.3);
        level.sendParticles(ParticleTypes.WITCH, px, py, pz, 20, 0.5, 0.7, 0.5, 0.05);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 2.0f), px, py, pz, 30, 0.6, 0.8, 0.6, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 15, 0.5, 0.7, 0.5, 0.1);

        level.playSound(null, px, py, pz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.3f, 1.5f);

        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();

        double tx = target.x;
        double ty = target.y + 1.0;
        double tz = target.z;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, tx, ty, tz, 60, 0.6, 1.0, 0.6, 0.15);
        level.sendParticles(ParticleTypes.PORTAL, tx, ty, tz, 40, 0.6, 1.0, 0.6, 0.3);
        level.sendParticles(ParticleTypes.WITCH, tx, ty, tz, 20, 0.5, 0.7, 0.5, 0.05);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 2.0f), tx, ty, tz, 30, 0.6, 0.8, 0.6, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 15, 0.5, 0.7, 0.5, 0.1);

        level.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.3f);
        level.playSound(null, tx, ty, tz, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    public static BlockPos findTargetPosition(Level level, Vec3 rawTarget) {
        ModConfig cfg = ModConfig.getInstance();
        int maxUp = cfg.getIntEffect("teleport_max_up", 5);
        int maxDown = cfg.getIntEffect("teleport_max_down", 5);

        int x = Mth.floor(rawTarget.x);
        int z = Mth.floor(rawTarget.z);
        int rawY = Mth.floor(rawTarget.y);
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        int searchTop = Math.min(rawY + maxUp, maxY);
        int searchBottom = Math.max(rawY - maxDown, minY);

        for (int y = rawY; y >= searchBottom; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isSolid(level, candidate)) continue;
            BlockPos standPos = candidate.above();
            if (!canStandAt(level, standPos)) continue;
            return standPos;
        }
        for (int y = rawY + 1; y <= searchTop; y++) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isSolid(level, candidate)) continue;
            BlockPos standPos = candidate.above();
            if (!canStandAt(level, standPos)) continue;
            return standPos;
        }
        return null;
    }

    private static boolean canStandAt(Level level, BlockPos pos) {
        return isPassable(level, pos) && isPassable(level, pos.above());
    }

    private static boolean isPassable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.is(BlockTags.LEAVES)) return true;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isSolid(Level level, BlockPos pos) {
        return !isPassable(level, pos);
    }

    public static Vec3 getHorizontalDirection(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            double yawRad = Math.toRadians(player.getYRot());
            horizontal = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        }
        return horizontal.normalize();
    }

    public static double computeDistance(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double maxDist = cfg.getEffect("teleport_max_distance", 30.0);
        double minDist = cfg.getEffect("teleport_min_distance", 3.0);
        double maxPitch = cfg.getEffect("teleport_max_pitch", -20.0);
        double minPitch = cfg.getEffect("teleport_min_pitch", 10.0);

        float pitch = player.getXRot();
        double t = (pitch - maxPitch) / (minPitch - maxPitch);
        t = Mth.clamp(t, 0.0, 1.0);
        return maxDist - t * (maxDist - minDist);
    }
}