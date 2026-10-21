package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class VitalityAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("vitality_bonus_hp");

    @Override public String getId() { return "bonus_health"; }
    @Override public String getDisplayName() { return "Живучесть"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/bonus_health"); }

    @Override
    public String getDescription() {
        int bonus = (int) ModConfig.getInstance().getEffect("vitality_bonus_hp", 5.0);
        return "Максимальное здоровье увеличено на " + bonus + " НР.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int bonus = (int) ModConfig.getInstance().getEffect("vitality_bonus_hp", 5.0);
        return List.of("§7Бонус HP: §c+" + bonus);
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double bonus = ModConfig.getInstance().getEffect("vitality_bonus_hp", 5.0);
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        if (existing == null || Math.abs(existing.amount() - bonus) > 1e-6) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }

        // Аура из сердец (раз в 15 тиков, чтобы было ненавязчиво)
        if (player.tickCount % 15 == 0) {
            ServerLevel level = player.level();
            level.sendParticles(ParticleTypes.HEART,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.2,
                    player.getY() + 0.8 + player.getRandom().nextDouble() * 0.8,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.2,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}