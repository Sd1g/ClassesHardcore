package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LuckySwapAbility implements Ability {
    private static final Random RNG = new Random();

    private static final Set<Item> BLACKLIST = Set.of(
            Items.AIR, Items.DEBUG_STICK,
            Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK,
            Items.COMMAND_BLOCK_MINECART, Items.STRUCTURE_BLOCK, Items.STRUCTURE_VOID,
            Items.JIGSAW, Items.BARRIER, Items.LIGHT, Items.KNOWLEDGE_BOOK,
            Items.END_PORTAL_FRAME, Items.BEDROCK,
            Items.DRAGON_EGG,
            Items.ENDER_DRAGON_SPAWN_EGG, Items.WITHER_SPAWN_EGG, Items.WARDEN_SPAWN_EGG
    );

    @Override public String getId() { return "lucky_swap"; }
    @Override public String getDisplayName() { return "Везучий обмен"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 12000; }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("lucky_swap", getBaseCooldown());
        int minutes = cd / 20 / 60;
        int seconds = (cd / 20) % 60;
        String timeText = minutes > 0
                ? (minutes + " мин." + (seconds > 0 ? " " + seconds + " сек." : ""))
                : (seconds + " сек.");
        return "Меняет предмет в руке на случайный, сохраняя количество. Кулдаун " + timeText + ".";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int cd = ModConfig.getInstance().getCooldown("lucky_swap", getBaseCooldown());
        int minutes = cd / 20 / 60;
        int seconds = (cd / 20) % 60;
        String timeText = minutes > 0
                ? (minutes + " мин." + (seconds > 0 ? " " + seconds + " сек." : ""))
                : (seconds + " сек.");
        return List.of("§7Кулдаун: §f" + timeText);
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cВ руке ничего нет — нечего обменивать."));
            playFailEffect(player);
            return false;
        }

        Item newItem = pickRandomItem();
        if (newItem == null) {
            player.sendSystemMessage(Component.literal("§cНе удалось найти замену. Попробуйте ещё раз."));
            playFailEffect(player);
            return false;
        }

        int maxStack = new ItemStack(newItem).getMaxStackSize();
        int newCount = Math.min(held.getCount(), maxStack);
        ItemStack newStack = new ItemStack(newItem, newCount);

        player.setItemInHand(InteractionHand.MAIN_HAND, newStack);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // ---- Успех: трёхцветный вихрь ----
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            int color = (i % 3 == 0) ? 0xFFD91A : (i % 3 == 1) ? 0xB34DFF : 0x33FFFF;
            level.sendParticles(new DustParticleOptions(color, 1.5f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        for (int i = 0; i < 6; i++) {
            level.sendParticles(new DustParticleOptions(0xFFD91A, 1.2f),
                    x, y + 0.3 + i * 0.25, z, 1, 0.15, 0, 0.15, 0);
        }

        level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 40, 0.8, 0.6, 0.8, 0.6);
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 25, 0.6, 0.7, 0.6, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 15, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 12, 0.5, 0.6, 0.5, 0.3);
        level.sendParticles(new DustParticleOptions(0xFFD91A, 1.8f), x, y + 1.0, z, 20, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 1.6f), x, y + 1.2, z, 18, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0x33FFFF, 1.4f), x, y + 1.4, z, 15, 0.6, 0.6, 0.6, 0.1);

        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.4f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1.2f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0f, 1.5f);

        player.sendOverlayMessage(Component.literal(
                "§dВезучий обмен. §fПолучено: " + newStack.getHoverName().getString() + " x" + newCount + "."));
        return true;
    }

    private static void playFailEffect(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(new DustParticleOptions(0xCC2222, 1.2f), x, y, z, 10, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 0.4, 0.4, 0.4, 0.03);

        level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private Item pickRandomItem() {
        List<Item> all = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BLACKLIST.contains(item)) all.add(item);
        }
        if (all.isEmpty()) return null;
        return all.get(RNG.nextInt(all.size()));
    }
}