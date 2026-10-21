package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ClassesHardcore;
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
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/climbing_toggle"); }

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

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        if (data.climbingActive) {
            // Красивый эффект включения: зеленые нити и искры
            level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f), x, y, z, 40, 0.5, 0.8, 0.5, 0.1);
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 20, 0.5, 0.5, 0.5, 0.5);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.3, 0.3, 0.3, 0.05);

            // Звук "хватки"
            level.playSound(null, x, y, z, SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);

            player.sendOverlayMessage(Component.literal("§aСцепление включено."));
        } else {
            player.removeEffect(MobEffects.LEVITATION);

            // Эффект выключения: рассеивание магии
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.4, 0.4, 0.4, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 10, 0.3, 0.3, 0.3, 0.02);

            level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.5f);

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

            // Эффект при подъеме по стене
            if (player.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.1, 0.2, 0.1, 0.01);
            }

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