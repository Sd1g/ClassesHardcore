package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class RichHarvestAbility implements Ability {
    @Override public String getId() { return "rich_harvest"; }
    @Override public String getDisplayName() { return "Богатый урожай"; }
    @Override public boolean isPassive() { return false; }

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

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -vert, -radius), center.offset(radius, vert, radius))) {
            BlockState state = level.getBlockState(pos);

            // ИСПРАВЛЕНИЕ: Проверяем только тег CROPS (культуры на грядках)
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