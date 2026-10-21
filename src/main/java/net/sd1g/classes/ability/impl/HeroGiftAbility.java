package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ClassesHardcore;
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
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/hero_gift"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("hero_gift", 12000);
    }

    public static int computeLevel(int trades, ModConfig cfg) {
        int perLevel = Math.max(1, cfg.getIntEffect("hero_trades_per_level", 50));
        return trades / perLevel;
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

        ServerLevel levelObj = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // ТОРЖЕСТВЕННЫЕ ЭФФЕКТЫ
        // 1. Золотой дождь (Тотем) и радость жителей
        levelObj.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 80, 0.8, 1.5, 0.8, 0.2);
        levelObj.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 40, 0.6, 0.6, 0.6, 0.1);
        // 2. Праздничный фейерверк
        levelObj.sendParticles(ParticleTypes.FIREWORK, x, y, z, 30, 1.0, 1.0, 1.0, 0.2);
        // 3. Золотые искры
        levelObj.sendParticles(new DustParticleOptions(0xFFD700, 2.5f), x, y, z, 50, 0.8, 0.8, 0.8, 0.2);

        // ТОРЖЕСТВЕННЫЕ ЗВУКИ
        // 1. Радость жителей
        levelObj.playSound(null, x, y, z, SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.5f, 1.0f);
        // 2. Церковный колокол (эпично и торжественно)
        levelObj.playSound(null, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.5f, 0.8f);
        // 3. Фанфары уровня
        levelObj.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

        player.sendOverlayMessage(Component.literal("§aДар героя. §fПолучено " + amount + " изумрудов."));
        return true;
    }
}