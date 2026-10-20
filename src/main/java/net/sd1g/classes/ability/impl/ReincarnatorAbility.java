package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ReincarnatorAbility implements Ability {
    @Override public String getId() { return "reincarnator"; }
    @Override public String getDisplayName() { return "Реинкарнация"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("reincarnator_totem_save_chance", 0.70) * 100);
        return "С шансом " + chance + "% сохраняет тотем при смерти. Нельзя носить незеритовую броню.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Тотем не тратится при срабатывании");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        // Проверяем и снимаем незеритовую броню
        checkAndRemoveNetherite(player, EquipmentSlot.HEAD);
        checkAndRemoveNetherite(player, EquipmentSlot.CHEST);
        checkAndRemoveNetherite(player, EquipmentSlot.LEGS);
        checkAndRemoveNetherite(player, EquipmentSlot.FEET);
    }

    private void checkAndRemoveNetherite(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (isNetherite(stack)) {
            // Возвращаем предмет в инвентарь (или выбрасываем, если места нет)
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.sendOverlayMessage(Component.literal("§cРеинкарнатор не может носить незеритовую броню!"));
        }
    }

    private boolean isNetherite(ItemStack stack) {
        return stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE) ||
                stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS);
    }
}