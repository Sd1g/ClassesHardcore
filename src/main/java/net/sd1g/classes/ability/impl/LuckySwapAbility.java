package net.sd1g.classes.ability.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        player.sendOverlayMessage(Component.literal("§dОбмен произведен!"));
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