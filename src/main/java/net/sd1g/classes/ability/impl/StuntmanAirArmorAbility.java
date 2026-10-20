package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class StuntmanAirArmorAbility implements Ability {
    private static final Identifier ARMOR_MOD_ID = ClassesHardcore.id("elytra_armor_bonus");
    private static final Identifier TOUGHNESS_MOD_ID = ClassesHardcore.id("elytra_toughness_bonus");

    @Override public String getId() { return "air_armor"; }
    @Override public String getDisplayName() { return "Воздушная броня"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/air_armor"); }

    @Override
    public String getDescription() {
        return "При ношении элитр они получают показатели защиты и твёрдости нагрудника из того же материала, что и остальные элементы брони.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает только с элитрами");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isElytra = chest.is(Items.ELYTRA);

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);

        if (armorAttr == null || toughAttr == null) return;

        if (!isElytra) {
            if (armorAttr.getModifier(ARMOR_MOD_ID) != null) armorAttr.removeModifier(ARMOR_MOD_ID);
            if (toughAttr.getModifier(TOUGHNESS_MOD_ID) != null) toughAttr.removeModifier(TOUGHNESS_MOD_ID);
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        // Определяем материал по шлему (или сапогам, если шлема нет)
        double armorVal = 0;
        double toughVal = 0;

        if (helmet.is(Items.NETHERITE_HELMET) || legs.is(Items.NETHERITE_LEGGINGS) || boots.is(Items.NETHERITE_BOOTS)) {
            armorVal = 8; toughVal = 3; // Незерит
        } else if (helmet.is(Items.DIAMOND_HELMET) || legs.is(Items.DIAMOND_LEGGINGS) || boots.is(Items.DIAMOND_BOOTS)) {
            armorVal = 8; toughVal = 2; // Алмаз
        } else if (helmet.is(Items.IRON_HELMET) || legs.is(Items.IRON_LEGGINGS) || boots.is(Items.IRON_BOOTS)) {
            armorVal = 6; toughVal = 0; // Железо
        } else if (helmet.is(Items.GOLDEN_HELMET) || legs.is(Items.GOLDEN_LEGGINGS) || boots.is(Items.GOLDEN_BOOTS)) {
            armorVal = 5; toughVal = 0; // Золото
        } else if (helmet.is(Items.CHAINMAIL_HELMET) || legs.is(Items.CHAINMAIL_LEGGINGS) || boots.is(Items.CHAINMAIL_BOOTS)) {
            armorVal = 5; toughVal = 0; // Кольчуга
        } else if (helmet.is(Items.LEATHER_HELMET) || legs.is(Items.LEATHER_LEGGINGS) || boots.is(Items.LEATHER_BOOTS)) {
            armorVal = 3; toughVal = 0; // Кожа
        }

        if (armorVal > 0) {
            armorAttr.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_MOD_ID, armorVal, AttributeModifier.Operation.ADD_VALUE));
            if (toughVal > 0) {
                toughAttr.addOrUpdateTransientModifier(new AttributeModifier(TOUGHNESS_MOD_ID, toughVal, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            if (armorAttr.getModifier(ARMOR_MOD_ID) != null) armorAttr.removeModifier(ARMOR_MOD_ID);
            if (toughAttr.getModifier(TOUGHNESS_MOD_ID) != null) toughAttr.removeModifier(TOUGHNESS_MOD_ID);
        }
    }
}