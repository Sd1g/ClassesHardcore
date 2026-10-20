package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class VitalityAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("vitality_bonus_hp");

    @Override public String getId() { return "bonus_health"; }
    @Override public String getDisplayName() { return "Живучесть"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/bonus_health"); }

    @Override
    public String getDescription() {
        return "Максимальное здоровье увеличено на 5 НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус HP: §c+5");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        if (existing == null || (int) existing.amount() != 5) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, 5.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}