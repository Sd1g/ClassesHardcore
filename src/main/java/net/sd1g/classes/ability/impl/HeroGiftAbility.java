package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;
import java.util.Random;

public class HeroGiftAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "hero_gift"; }
    @Override public String getDisplayName() { return "Дар героя"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("hero_gift", 12000);
    }

    public static int computeLevel(int trades, ModConfig cfg) {
        int perLevel = cfg.getIntEffect("hero_trades_per_level", 50);
        return perLevel > 0 ? trades / perLevel : 0;
    }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("hero_gift", 12000) / 1200;
        return "Раз в " + cd + " минут получает изумруды и шанс на редкие предметы (зависит от уровня).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int trades = AbilityClientState.heroTrades;
        int level = computeLevel(trades, cfg);

        int min = (int) (cfg.getEffect("hero_gift_min_base", 1.0) + level * cfg.getEffect("hero_gift_min_per_50", 2.0));
        int max = (int) (cfg.getEffect("hero_gift_max_base", 5.0) + level * cfg.getEffect("hero_gift_max_per_50", 5.0));

        return List.of(
                "§7Награда: §a" + min + "–" + max + " изумрудов",
                "§7Ур. V: §dтотем " + (int)(cfg.getEffect("hero_totem_chance_at_5", 0.03)*100) + "%",
                "§7Ур. X: §dтотем " + (int)(cfg.getEffect("hero_totem_chance_at_10", 0.06)*100) + "% §7+ §eяблоко " + (int)(cfg.getEffect("hero_enchanted_golden_apple_chance_at_10", 0.01)*100) + "%"
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int level = computeLevel(data.heroTrades, cfg);

        int min = (int) (cfg.getEffect("hero_gift_min_base", 1.0) + level * cfg.getEffect("hero_gift_min_per_50", 2.0));
        int max = (int) (cfg.getEffect("hero_gift_max_base", 5.0) + level * cfg.getEffect("hero_gift_max_per_50", 5.0));
        int amount = min + RNG.nextInt(Math.max(1, max - min + 1));

        ItemStack emeralds = new ItemStack(Items.EMERALD, amount);
        if (!player.getInventory().add(emeralds)) player.drop(emeralds, false);

        if (level >= 5) {
            double totemChance = (level >= 10) ? cfg.getEffect("hero_totem_chance_at_10", 0.06) : cfg.getEffect("hero_totem_chance_at_5", 0.03);
            if (RNG.nextDouble() < totemChance) {
                ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
                if (!player.getInventory().add(totem)) player.drop(totem, false);
                player.sendOverlayMessage(Component.literal("§dГерой деревни! §f+1 Тотем."));
            }
        }

        if (level >= 10) {
            if (RNG.nextDouble() < cfg.getEffect("hero_enchanted_golden_apple_chance_at_10", 0.01)) {
                ItemStack gap = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
                if (!player.getInventory().add(gap)) player.drop(gap, false);
                player.sendOverlayMessage(Component.literal("§6Герой деревни X! §f+1 Зачарованное яблоко."));
            }
        }

        ServerLevel world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.8f, 1.0f);
        player.sendOverlayMessage(Component.literal("§aДар героя. §fПолучено " + amount + " изумрудов."));
        return true;
    }
}