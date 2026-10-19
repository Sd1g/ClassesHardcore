package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class RichHarvestAbility implements Ability {
    @Override public String getId() { return "rich_harvest"; }
    @Override public String getDisplayName() { return "Богатый урожай"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 3600; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int r = (int) cfg.getEffect("rich_harvest_radius", 50.0);
        int cdMin = cfg.getCooldown("rich_harvest", getBaseCooldown()) / 20 / 60;
        return "Ускоряет рост культур, саженцев и тростника в радиусе " + r + " блоков. Кулдаун " + cdMin + " мин.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int r = (int) cfg.getEffect("rich_harvest_radius", 50.0);
        int cd = cfg.getCooldown("rich_harvest", getBaseCooldown());
        return List.of(
                "§7Радиус: §f" + r + " блоков",
                "§7Кулдаун: §f" + (cd / 20 / 60) + " мин."
        );
    }

    private static boolean isAllowedBlock(BlockState state) {
        var block = state.getBlock();
        if (block instanceof CropBlock) return true;
        if (block instanceof StemBlock) return true;
        if (block instanceof SaplingBlock) return true;
        if (block instanceof NetherWartBlock) return true;
        if (block instanceof CocoaBlock) return true;
        if (block instanceof SweetBerryBushBlock) return true;
        if (block == Blocks.SUGAR_CANE) return true;
        return false;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int radius = (int) cfg.getEffect("rich_harvest_radius", 50.0);
        int vRange = (int) cfg.getEffect("rich_harvest_vertical_range", 10.0);

        ServerLevel level = player.level();
        int cx = player.getBlockX();
        int cy = player.getBlockY();
        int cz = player.getBlockZ();

        int minX = cx - radius, maxX = cx + radius;
        int minZ = cz - radius, maxZ = cz + radius;
        int minY = Math.max(level.getMinY(), cy - vRange);
        int maxY = Math.min(level.getMaxY(), cy + vRange);

        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (isAllowedBlock(state)) candidates.add(pos.immutable());
        }

        int grown = 0;
        for (BlockPos pos : candidates) {
            BlockState state = level.getBlockState(pos);
            if (!isAllowedBlock(state)) continue;

            if (state.getBlock() == Blocks.SUGAR_CANE) {
                if (growSugarCane(level, pos)) grown++;
                continue;
            }

            if (state.getBlock() instanceof BonemealableBlock bm
                    && bm.isValidBonemealTarget(level, pos, state)) {
                bm.performBonemeal(level, level.getRandom(), pos, state);
                grown++;
            }
        }

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // Восходящий зелёный столб
        for (int i = 0; i < 10; i++) {
            level.sendParticles(new DustParticleOptions(0x33FF33, 1.3f),
                    px, py + 0.3 + i * 0.25, pz, 1, 0.2, 0, 0.2, 0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py + 1.0, pz, 60, 4.0, 1.5, 4.0, 0.1);
        level.sendParticles(new DustParticleOptions(0x33FF33, 2.0f), px, py + 1.0, pz, 50, 4.0, 1.5, 4.0, 0.15);
        level.sendParticles(new DustParticleOptions(0xD9A400, 1.6f), px, py + 1.5, pz, 30, 4.0, 1.5, 4.0, 0.1);
        level.sendParticles(ParticleTypes.END_ROD, px, py + 1.0, pz, 25, 4.0, 1.5, 4.0, 0.05);

        // Кольцо радиусом 50
        int ringPoints = 80;
        for (int i = 0; i < ringPoints; i++) {
            double angle = i * Math.PI * 2 / ringPoints;
            double rx = px + Math.cos(angle) * radius;
            double rz = pz + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, rx, py, rz, 2, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(new DustParticleOptions(0x33FF33, 1.5f), rx, py, rz, 1, 0, 0, 0, 0);
        }

        level.playSound(null, px, py, pz, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.8f, 0.7f);
        level.playSound(null, px, py, pz, SoundEvents.BONE_MEAL_USE,
                SoundSource.PLAYERS, 2.0f, 0.7f);

        player.sendOverlayMessage(Component.literal(
                "§aБогатый урожай. §fОбработано блоков: " + grown + "."));
        return true;
    }

    private static boolean growSugarCane(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos.above()).is(Blocks.SUGAR_CANE)) return false;
        int height = 1;
        BlockPos cursor = pos;
        while (level.getBlockState(cursor.below()).is(Blocks.SUGAR_CANE)) {
            height++;
            cursor = cursor.below();
            if (height > 3) return false;
        }
        if (height >= 3) return false;
        BlockPos placeAt = pos.above();
        BlockState atPlace = level.getBlockState(placeAt);
        if (atPlace.isAir() || atPlace.canBeReplaced()) {
            level.setBlockAndUpdate(placeAt, Blocks.SUGAR_CANE.defaultBlockState());
            return true;
        }
        return false;
    }
}