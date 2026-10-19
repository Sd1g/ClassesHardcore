package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class CombatExperienceAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("combat_experience_bonus");

    @Override public String getId() { return "combat_experience"; }
    @Override public String getDisplayName() { return "Боевой опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_experience"); }

    @Override
    public String getDescription() {
        return "Наносит мечом на 15% больше итогового физического урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус: §c+15% урона §7(только мечом)");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        boolean hasSword = mainHand.is(ItemTags.SWORDS);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);

        if (hasSword && existing == null) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!hasSword && existing != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}