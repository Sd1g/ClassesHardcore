package net.sd1g.classes.util;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.sd1g.classes.ClassesHardcore;

import java.util.ArrayList;
import java.util.List;

/**
 * Единая точка изменения атрибутов игрока.
 * Все способности работают через этот класс — никаких прямых вызовов
 * attr.addOrUpdateTransientModifier в коде способностей.
 */
public final class AttributesHelper {
    private static final List<Holder<Attribute>> TRACKED = List.of(
            Attributes.MAX_HEALTH,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.MOVEMENT_SPEED,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.JUMP_STRENGTH
    );

    private AttributesHelper() {}

    public static void add(ServerPlayer player, Holder<Attribute> attr, String key, double amount,
                           AttributeModifier.Operation op) {
        if (amount == 0) { remove(player, attr, key); return; }
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        var id = ClassesHardcore.id(key);
        var existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 1e-6) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        if (attr == Attributes.MAX_HEALTH) clampHealth(player);
    }

    public static void remove(ServerPlayer player, Holder<Attribute> attr, String key) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(ClassesHardcore.id(key));
        if (attr == Attributes.MAX_HEALTH) clampHealth(player);
    }

    /** Снимает ВСЕ модификаторы с namespace classeshardcore со всех отслеживаемых атрибутов. */
    public static void clearAll(ServerPlayer player) {
        for (Holder<Attribute> attr : TRACKED) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) continue;
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
                if (m.id().getNamespace().equals(ClassesHardcore.MOD_ID)) inst.removeModifier(m);
            }
        }
        clampHealth(player);
    }

    /** Снять все модификаторы с конкретного атрибута от нашего мода. */
    public static void clearFrom(ServerPlayer player, Holder<Attribute> attr) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
            if (m.id().getNamespace().equals(ClassesHardcore.MOD_ID)) inst.removeModifier(m);
        }
        if (attr == Attributes.MAX_HEALTH) clampHealth(player);
    }

    public static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}