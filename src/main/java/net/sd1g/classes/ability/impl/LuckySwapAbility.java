package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ClassesHardcore;
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

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/lucky_swap"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("lucky_swap", 12000);
    }

    @Override
    public String getDescription() {
        int cd = ModConfig.getInstance().getCooldown("lucky_swap", 12000) / 1200;
        return "Меняет предмет в руке на случайный. Перезарядка: " + cd + " мин.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Сохранение количества");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cВ руке ничего нет."));
            return false;
        }

        Item newItem = pickRandomItem();
        if (newItem == null) {
            player.sendSystemMessage(Component.literal("§cНе удалось найти замену."));
            return false;
        }

        int maxStack = new ItemStack(newItem).getMaxStackSize();
        int newCount = Math.min(held.getCount(), maxStack);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(newItem, newCount));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // ЭФФЕКТЫ КАЗИНО (Джекпот)
        // 1. Взрыв золотых искр (монеты)
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.6, 0.6, 0.6, 0.2);
        // 2. Фейерверк (конфетти)
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 40, 1.0, 1.0, 1.0, 0.2);
        // 3. Белые искры (свет от автомата)
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
        // 4. Зеленые искры (удача)
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 15, 0.4, 0.4, 0.4, 0.02);

        // ЗВУКИ КАЗИНО
        // 1. Рычаг (клик кнопки)
        level.playSound(null, x, y, z, SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 1.0f, 1.0f);
        // 2. Звон слотов (колокольчики)
        level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);
        // 3. Джекпот (фанфары)
        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        // 4. Звон монет
        level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);

        player.sendOverlayMessage(Component.literal("§dДжекпот! Обмен произведен!"));
        return true;
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