package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ReincarnatorAbility implements Ability {
    @Override public String getId() { return "reincarnator"; }
    @Override public String getDisplayName() { return "Реинкарнация"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/reincarnator"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("reincarnator_totem_save_chance", 0.70) * 100);
        int maxSaves = cfg.getIntEffect("reincarnator_max_consecutive_saves", 10);
        return "С шансом " + chance + "% сохраняет тотем при смерти (до " + maxSaves + " раз подряд). Нельзя носить незеритовую броню.";
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
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
            player.setItemSlot(slot, ItemStack.EMPTY);

            ServerLevel level = player.level();
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);

            player.sendOverlayMessage(Component.literal("§cРеинкарнатор не может носить незеритовую броню!"));
        }
    }

    private boolean isNetherite(ItemStack stack) {
        return stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE) ||
                stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS);
    }
}