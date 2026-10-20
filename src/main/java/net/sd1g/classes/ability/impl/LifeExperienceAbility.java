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

public class LifeExperienceAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("life_experience_bonus_hp");

    @Override public String getId() { return "life_experience"; }
    @Override public String getDisplayName() { return "Жизненный опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/life_experience"); }

    @Override
    public String getDescription() {
        return "За каждые 5 уровней опыта максимальное здоровье увеличивается на 1 НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int bonus = player.experienceLevel / 5;
        int toNext = 5 - (player.experienceLevel % 5);
        return List.of("§7Текущий бонус: §c+" + bonus + " HP", "§7До след. шага: §f" + toNext + " ур.");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double targetBonus = player.experienceLevel / 5.0;
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;

        if (Math.abs(oldBonus - targetBonus) > 1e-6) {
            if (targetBonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, targetBonus, AttributeModifier.Operation.ADD_VALUE));
            } else if (existing != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}