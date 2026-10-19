package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;
import java.util.Random;

public class LuckyDiggerFortuneHandler {

    private static final List<Block> ORES = List.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    private static final Random RANDOM = new Random();

    /** Проверяет, является ли блок рудой из нашего списка. */
    public static boolean isOre(Block block) {
        return ORES.contains(block);
    }

    public static void register() {
        LootTableEvents.MODIFY_DROPS.register(LuckyDiggerFortuneHandler::onModifyDrops);
    }

    private static void onModifyDrops(Holder<LootTable> lootTable,
                                      LootContext context,
                                      List<ItemStack> drops) {
        ServerPlayer player = getPlayerFromContext(context);
        if (player == null) return;

        PlayerClassData data = PlayerClassData.get(player);
        if (!"lucky_digger".equals(data.playerClass)) return;
        if (!"luck".equals(data.luckyDiggerMode)) return;
        if (data.luckyDiggerLevel <= 0) return;

        Block brokenBlock = getBrokenBlockFromContext(context);
        if (brokenBlock == null || !ORES.contains(brokenBlock)) return;

        applyFortuneEffect(drops, data.luckyDiggerLevel);
    }

    private static ServerPlayer getPlayerFromContext(LootContext context) {
        try {
            Object entity = context.getParameter(LootContextParams.THIS_ENTITY);
            if (entity instanceof ServerPlayer sp) return sp;
        } catch (Exception ignored) {}
        return null;
    }

    private static Block getBrokenBlockFromContext(LootContext context) {
        try {
            var state = context.getParameter(LootContextParams.BLOCK_STATE);
            return state.getBlock();
        } catch (Exception ignored) {}
        return null;
    }

    private static void applyFortuneEffect(List<ItemStack> drops, int fortuneLevel) {
        ModConfig cfg = ModConfig.getInstance();
        double extraChancePerLevel = cfg.getEffect("fortune_extra_drop_chance_per_level", 0.33);
        int maxExtraPerRoll = cfg.getIntEffect("fortune_max_extra_drops_per_level", 1);

        List<ItemStack> originalDrops = List.copyOf(drops);

        for (ItemStack stack : originalDrops) {
            int extra = 0;
            for (int i = 0; i < fortuneLevel; i++) {
                if (RANDOM.nextDouble() < extraChancePerLevel) {
                    extra += 1 + RANDOM.nextInt(Math.max(1, maxExtraPerRoll));
                }
            }
            if (extra > 0) {
                ItemStack bonus = stack.copy();
                bonus.setCount(extra);
                drops.add(bonus);
            }
        }
    }
}