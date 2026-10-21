package net.sd1g.classes.ability.impl;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class RichHarvestAbility implements Ability {
    @Override public String getId() { return "rich_harvest"; }
    @Override public String getDisplayName() { return "Богатый урожай"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/rich_harvest"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("rich_harvest", 3600);
    }

    @Override
    public String getDescription() {
        int cd = getBaseCooldown() / 20;
        return "Ускоряет рост культур на вспаханной земле. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект костной муки (только для культур)");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int radius = cfg.getIntEffect("rich_harvest_radius", 50);
        int vert = cfg.getIntEffect("rich_harvest_vertical_range", 10);

        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        int count = 0;

        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // МАССИВНАЯ АУРА ВОКРУГ ИГРОКА
        // 1. Огромное количество зеленых искр и пыльцы
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 200, radius / 2.0, 2.0, radius / 2.0, 0.2);
        level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 100, radius / 2.0, 1.5, radius / 2.0, 0.1);

        // 2. Светящаяся зеленая магия (волна жизни)
        level.sendParticles(new DustParticleOptions(0x00FF00, 2.5f), x, y, z, 100, radius / 2.0, 1.0, radius / 2.0, 0.1);

        // 3. Магические искры, поднимающиеся вверх
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 50, radius / 3.0, 1.0, radius / 3.0, 0.1);

        // Звук бурного роста
        level.playSound(null, x, y, z, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);

        // Логика ускорения роста
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -vert, -radius), center.offset(radius, vert, radius))) {
            BlockState state = level.getBlockState(pos);

            if (state.is(BlockTags.CROPS) && state.getBlock() instanceof BonemealableBlock bonemealable) {
                if (bonemealable.isValidBonemealTarget(level, pos, state)) {
                    if (bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                        bonemealable.performBonemeal(level, level.getRandom(), pos, state);
                        count++;
                    }
                }
            }
        }
        player.sendOverlayMessage(Component.literal("§aБогатый урожай! §fУскорено культур: " + count));
        return true;
    }
}