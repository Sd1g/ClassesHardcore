package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ReincarnatorAbility implements Ability {
    @Override public String getId() { return "reincarnation"; }
    @Override public String getDisplayName() { return "Реинкарнация"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(
                ModConfig.getInstance().getEffect("reincarnator_totem_save_chance", 0.80) * 100);
        return "Тотем бессмертия с шансом " + chance + "% не тратится при срабатывании. "
                + "Запрещена незеритовая броня.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("reincarnator_totem_save_chance", 0.80) * 100);
        return List.of(
                "§7Шанс сохранить тотем: §a" + chance + "%",
                "§7Незеритовая броня: §cзапрещена"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        removeNetheriteArmor(player);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Фиолетовая пыль у ног
        if (player.tickCount % 15 == 0) {
            level.sendParticles(new DustParticleOptions(0xFF4DFF, 0.9f),
                    x, y + 0.1, z, 2, 0.4, 0.05, 0.4, 0.02);
        }
        // Столб фиолетово-золотой
        if (player.tickCount % 25 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.4 + i * 0.6;
                int color = (i % 2 == 0) ? 0xFF4DFF : 0xFFAA00; // int, не double!
                level.sendParticles(new DustParticleOptions(color, 0.7f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        // Магические искры опыта высоко над головой
        if (player.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.4, z, 2, 0.4, 0.3, 0.4, 0.1);
        }
        // Редкие души
        if (player.tickCount % 60 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 1.0, z, 2, 0.3, 0.2, 0.3, 0.01);
        }
    }

    private static void removeNetheriteArmor(ServerPlayer player) {
        boolean removed = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (!isNetheriteArmor(stack)) continue;

            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            removed = true;
        }

        if (removed) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(new DustParticleOptions(0xFF4DFF, 1.5f), x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(new DustParticleOptions(0xFFAA00, 1.2f), x, y + 0.3, z, 12, 0.5, 0.5, 0.5, 0.08);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.3, z, 15, 0.5, 0.5, 0.5, 0.3);

            level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_NETHERITE,
                    SoundSource.PLAYERS, 1.2f, 0.6f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0f, 1.7f);

            player.sendOverlayMessage(Component.literal(
                    "§dРеинкарнатор не может носить незеритовую броню."));
        }
    }

    private static boolean isNetheriteArmor(ItemStack stack) {
        return stack.is(Items.NETHERITE_HELMET)
                || stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.NETHERITE_LEGGINGS)
                || stack.is(Items.NETHERITE_BOOTS);
    }
}