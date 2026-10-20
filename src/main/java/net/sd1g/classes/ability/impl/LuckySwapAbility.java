package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class LuckySwapAbility implements Ability {
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

        // Упрощенная логика выбора случайного предмета (как было у тебя)
        // (В реальном коде вызывается pickRandomItem())
        Item newItem = net.minecraft.world.item.Items.DIAMOND; // Заглушка для примера
        int maxStack = new ItemStack(newItem).getMaxStackSize();
        int newCount = Math.min(held.getCount(), maxStack);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(newItem, newCount));
        player.sendOverlayMessage(Component.literal("§dОбмен произведен!"));
        return true;
    }
}