package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.CooldownSyncPayload;

import java.util.List;

public class TeleporterAbility implements Ability {
    @Override public String getId() { return "teleport"; }
    @Override public String getDisplayName() { return "Телепортация"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/teleport"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("teleport", 100);
    }

    public static double getMaxDistance(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int tpPerBonus = Math.max(1, cfg.getIntEffect("teleporter_tp_per_bonus", 200));
        double maxBonus = cfg.getEffect("teleporter_max_bonus", 10.0);
        double baseMax = cfg.getEffect("teleport_max_distance", 30.0);

        double bonus = Math.min((data.teleporterCount / (double)tpPerBonus), maxBonus);
        return baseMax + bonus;
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int perBlock = cfg.getIntEffect("teleport_cooldown_per_block", 40);
        int minCd = cfg.getIntEffect("teleport_min_cooldown", 200) / 20;
        int maxCd = cfg.getIntEffect("teleport_max_cooldown", 1200) / 20;
        return "Перемещает по взгляду на " + (int)cfg.getEffect("teleport_min_distance", 3.0) + "–" + (int)cfg.getEffect("teleport_max_distance", 30.0) + " блоков. Перезарядка: " + (perBlock/20.0) + " сек/блок (от " + minCd + " до " + maxCd + " сек).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        if (player instanceof ServerPlayer sp) {
            ModConfig cfg = ModConfig.getInstance();
            double maxDist = getMaxDistance(sp);
            PlayerClassData data = PlayerClassData.get(sp);
            int perBlock = cfg.getIntEffect("teleport_cooldown_per_block", 40);
            int minCd = cfg.getIntEffect("teleport_min_cooldown", 200);
            int maxCd = cfg.getIntEffect("teleport_max_cooldown", 1200);

            return List.of(
                    "§7Максимальная дальность: §f" + (int)maxDist + " блоков",
                    "§7Успешных телепортаций: §b" + data.teleporterCount,
                    "§7КД: §f" + (perBlock / 20.0) + " сек/блок (от " + (minCd/20) + " до " + (maxCd/20) + " сек)"
            );
        }
        return List.of();
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double distance = computeDistance(player);

        int perBlock = cfg.getIntEffect("teleport_cooldown_per_block", 40);
        int minCd = cfg.getIntEffect("teleport_min_cooldown", 200);
        int maxCd = cfg.getIntEffect("teleport_max_cooldown", 1200);

        int calculated = (int) (distance * perBlock);
        return Math.max(minCd, Math.min(calculated, maxCd));
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        Vec3 horizontalDir = getHorizontalDirection(player);
        Vec3 eye = player.getEyePosition();
        double distance = computeDistance(player);
        Vec3 rawTarget = eye.add(horizontalDir.scale(distance));

        ServerLevel level = player.level();
        BlockPos standBp = findTargetPosition(level, rawTarget);
        if (standBp == null) {
            player.sendOverlayMessage(Component.literal("§cЗдесь телепортироваться нельзя."));
            return false;
        }

        Vec3 target = new Vec3(rawTarget.x, standBp.getY(), rawTarget.z);
        double px = player.getX(), py = player.getY() + 1.0, pz = player.getZ();

        spawnTeleportBurst(level, px, py, pz);

        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();

        double tx = target.x, ty = target.y + 1.0, tz = target.z;
        spawnTeleportBurst(level, tx, ty, tz);

        PlayerClassData data = PlayerClassData.get(player);
        data.teleporterCount++;
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new CooldownSyncPayload("teleporter_count", data.teleporterCount));
        return true;
    }

    private void spawnTeleportBurst(ServerLevel level, double x, double y, double z) {
        // Измененный эффект: Космический голубой/бирюзовый вихрь
        level.sendParticles(new DustParticleOptions(0x00FFFF, 1.5f), x, y, z, 50, 0.4, 0.8, 0.4, 0.1); // Ярко-голубой
        level.sendParticles(new DustParticleOptions(0x1E90FF, 2.0f), x, y, z, 30, 0.3, 0.5, 0.3, 0.05); // Морская волна
        level.sendParticles(new DustParticleOptions(0x0000FF, 1.0f), x, y, z, 20, 0.5, 0.5, 0.5, 0.02); // Глубокий синий
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.2, 0.2, 0.2, 0.05); // Белые искры
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 15, 0.4, 0.4, 0.4, 0.1); // Критические искры

        // Звук оставляем как ты просил
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    public static BlockPos findTargetPosition(Level level, Vec3 rawTarget) {
        ModConfig cfg = ModConfig.getInstance();
        int maxUp = cfg.getIntEffect("teleport_max_up", 5);
        int maxDown = cfg.getIntEffect("teleport_max_down", 5);

        int x = Mth.floor(rawTarget.x);
        int z = Mth.floor(rawTarget.z);
        int rawY = Mth.floor(rawTarget.y);
        int minY = level.getMinY(), maxY = level.getMaxY();
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
        double maxDist = (player instanceof ServerPlayer sp) ? getMaxDistance(sp) : cfg.getEffect("teleport_max_distance", 30.0);
        double minDist = cfg.getEffect("teleport_min_distance", 3.0);
        double maxPitch = cfg.getEffect("teleport_max_pitch", -20.0);
        double minPitch = cfg.getEffect("teleport_min_pitch", 10.0);

        float pitch = player.getXRot();
        double t = Mth.clamp((pitch - maxPitch) / (minPitch - maxPitch), 0.0, 1.0);
        return maxDist - t * (maxDist - minDist);
    }
}