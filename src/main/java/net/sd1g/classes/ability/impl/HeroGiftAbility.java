package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
    @Override public int getBaseCooldown() { return 12000; }

    public static int computeHeroLevel(int trades) {
        return Math.min(9, trades / 50);
    }

    private static int computeMin(int level) {
        ModConfig cfg = ModConfig.getInstance();
        int base = (int) cfg.getEffect("hero_gift_min_base", 1.0);
        int per50 = (int) cfg.getEffect("hero_gift_min_per_50", 2.0);
        return base + level * per50;
    }

    private static int computeMax(int level) {
        ModConfig cfg = ModConfig.getInstance();
        int base = (int) cfg.getEffect("hero_gift_max_base", 5.0);
        int per50 = (int) cfg.getEffect("hero_gift_max_per_50", 5.0);
        return base + level * per50;
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("hero_gift_min_base", 1.0);
        int max = (int) cfg.getEffect("hero_gift_max_base", 5.0);
        int cdMin = cfg.getCooldown("hero_gift", getBaseCooldown()) / 20 / 60;
        return "Раз в " + cdMin + " мин даёт " + min + "–" + max + " изумрудов. "
                + "Каждые 50 сделок: минимум +2, максимум +5. "
                + "Ур. V — шанс тотема, ур. X — тотем и зачарованное золотое яблоко.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int trades = AbilityClientState.heroTrades;
        int level = computeHeroLevel(trades);
        int min = computeMin(level);
        int max = computeMax(level);
        ModConfig cfg = ModConfig.getInstance();
        int cd = cfg.getCooldown("hero_gift", getBaseCooldown());

        return List.of(
                "§7Сделок: §f" + trades + " §7(уровень §f" + level + "§7)",
                "§7Награда: §a" + min + "–" + max + " изумрудов",
                "§7Кулдаун: §f" + (cd / 20 / 60) + " мин.",
                "§7Ур. V: §dтотем " + (int) Math.round(cfg.getEffect("hero_totem_chance_at_5", 0.03) * 100) + "%",
                "§7Ур. X: §dтотем " + (int) Math.round(cfg.getEffect("hero_totem_chance_at_10", 0.06) * 100)
                        + "% §7+ §eзол. яблоко " + (int) Math.round(cfg.getEffect("hero_enchanted_golden_apple_chance_at_10", 0.03) * 100) + "%"
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        int level = computeHeroLevel(data.heroTrades);

        int min = computeMin(level);
        int max = computeMax(level);
        if (max < min) max = min;
        int amount = min + RNG.nextInt(max - min + 1);

        ItemStack emeralds = new ItemStack(Items.EMERALD, amount);
        if (!player.getInventory().add(emeralds)) player.drop(emeralds, false);

        // Ур. V: шанс тотема
        if (level >= 5) {
            double totemChance = (level >= 9)
                    ? cfg.getEffect("hero_totem_chance_at_10", 0.06)
                    : cfg.getEffect("hero_totem_chance_at_5", 0.03);
            if (RNG.nextDouble() < totemChance) {
                ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
                if (!player.getInventory().add(totem)) player.drop(totem, false);
                player.sendOverlayMessage(Component.literal("§dГерой деревни! §f+1 Тотем бессмертия."));
            }
        }

        // Ур. X: шанс зачарованного золотого яблока
        if (level >= 9) {
            double gapChance = cfg.getEffect("hero_enchanted_golden_apple_chance_at_10", 0.03);
            if (RNG.nextDouble() < gapChance) {
                ItemStack gap = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
                if (!player.getInventory().add(gap)) player.drop(gap, false);
                player.sendOverlayMessage(Component.literal("§6Герой деревни X! §f+1 Зачарованное золотое яблоко."));
            }
        }

        ServerLevel world = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Золотой столб
        for (int i = 0; i < 10; i++) {
            world.sendParticles(new DustParticleOptions(0xFFD91A, 1.3f),
                    x, y + 0.3 + i * 0.25, z, 1, 0.2, 0, 0.2, 0);
        }
        world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 40, 0.7, 0.9, 0.7, 0.5);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 30, 0.7, 0.7, 0.7, 0.1);
        world.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 20, 0.6, 0.6, 0.6, 0.05);
        world.sendParticles(new DustParticleOptions(0xFFD91A, 2.0f), x, y + 1.0, z, 40, 0.7, 0.8, 0.7, 0.1);
        world.sendParticles(new DustParticleOptions(0x1AE64D, 1.8f), x, y + 1.2, z, 35, 0.7, 0.8, 0.7, 0.1);
        world.sendParticles(ParticleTypes.ENCHANT, x, y + 1.4, z, 25, 0.8, 0.8, 0.8, 0.5);

        world.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.8f, 1.0f);
        world.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5f, 1.3f);
        world.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 1.6f);
        world.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.5f, 1.5f);

        player.sendOverlayMessage(Component.literal(
                "§aДар героя. §fПолучено " + amount + " изумруд" + pluralSuffix(amount) + "."));
        return true;
    }

    private static String pluralSuffix(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "а";
        return "ов";
    }
}