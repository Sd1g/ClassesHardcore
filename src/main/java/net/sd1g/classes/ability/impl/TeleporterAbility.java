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
    @Override public int getBaseCooldown() { return 200; } // 10 сек (дефолт)

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/teleport"); }

    public static double getMaxDistance(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        double bonus = Math.min(data.teleporterCount / 200.0, 10.0);
        return 30.0 + bonus;
    }

    @Override
    public String getDescription() {
        return "Перемещает по направлению взгляда на 3–30 блоков. Перезарядка зависит от дистанции.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        if (player instanceof ServerPlayer sp) {
            double maxDist = getMaxDistance(sp);
            PlayerClassData data = PlayerClassData.get(sp);

            ModConfig cfg = ModConfig.getInstance();
            int perBlockTicks = cfg.getCooldown("teleport_cooldown_per_block", 40);
            int minCdTicks = cfg.getCooldown("teleport_min_cooldown", 200);
            int maxCdTicks = cfg.getCooldown("teleport_max_cooldown", 1200);

            return List.of(
                    "§7Максимальная дальность: §f" + (int)maxDist + " блоков",
                    "§7Успешных телепортаций: §b" + data.teleporterCount,
                    "§7КД: §f" + (perBlockTicks / 20.0) + " сек/блок (от " + (minCdTicks/20) + " до " + (maxCdTicks/20) + " сек)"
            );
        }
        return List.of();
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double distance = computeDistance(player);

        // Читаем параметры из конфига
        int perBlock = cfg.getCooldown("teleport_cooldown_per_block", 40);
        int minCd = cfg.getCooldown("teleport_min_cooldown", 200);
        int maxCd = cfg.getCooldown("teleport_max_cooldown", 1200);

        int calculated = (int) (distance * perBlock);

        // Ограничиваем значениями мин и макс
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
            return false; // Не уходит в КД при провале
        }

        Vec3 target = new Vec3(rawTarget.x, standBp.getY(), rawTarget.z);
        double px = player.getX(), py = player.getY() + 1.0, pz = player.getZ();

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 60, 0.6, 1.0, 0.6, 0.15);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 2.0f), px, py, pz, 30, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, px, py, pz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.0f);

        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();

        double tx = target.x, ty = target.y + 1.0, tz = target.z;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, tx, ty, tz, 60, 0.6, 1.0, 0.6, 0.15);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 2.0f), tx, ty, tz, 30, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.3f);

        // Увеличиваем счетчик и синхронизируем с клиентом
        PlayerClassData data = PlayerClassData.get(player);
        data.teleporterCount++;
        PlayerClassData.markDirty(player);

        // Синхронизация для тултипа
        ServerPlayNetworking.send(player, new CooldownSyncPayload("teleporter_count", data.teleporterCount));

        return true;
    }

    public static BlockPos findTargetPosition(Level level, Vec3 rawTarget) {
        int maxUp = 5, maxDown = 5;
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
        double maxDist = (player instanceof ServerPlayer sp) ? getMaxDistance(sp) : 30.0;
        double minDist = 3.0;
        double maxPitch = -20.0, minPitch = 10.0;
        float pitch = player.getXRot();
        double t = Mth.clamp((pitch - maxPitch) / (minPitch - maxPitch), 0.0, 1.0);
        return maxDist - t * (maxDist - minDist);
    }
}