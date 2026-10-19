package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int bonus = ModConfig.getInstance().getIntEffect("vitality_bonus_hp", 10);
        return "Даёт +" + bonus + " HP к максимальному здоровью.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int bonus = ModConfig.getInstance().getIntEffect("vitality_bonus_hp", 10);
        return List.of(
                "§7Бонус HP: §c+" + bonus,
                "§7Максимум: §c" + (int) player.getMaxHealth() + " HP"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        int bonus = ModConfig.getInstance().getIntEffect("vitality_bonus_hp", 10);

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        boolean wasAbsent = existing == null;
        boolean wrongAmount = existing != null && (int) existing.amount() != bonus;
        boolean needsChange = wasAbsent || wrongAmount;

        if (needsChange) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, (double) bonus, AttributeModifier.Operation.ADD_VALUE));
        }
        // Звук только при первой выдаче
        if (wasAbsent) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();
            level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.sendParticles(ParticleTypes.HEART, x, y, z, 10, 0.5, 0.4, 0.5, 0.03);
            level.sendParticles(new DustParticleOptions(0xFF4D4D, 1.3f), x, y, z, 12, 0.5, 0.5, 0.5, 0.08);
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Сердце над головой раз в 30 тиков
        if (player.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.HEART, x, y + 1.5, z, 2, 0.4, 0.2, 0.4, 0.02);
        }
        // Красное кольцо у ног раз в 25 тиков
        if (player.tickCount % 25 == 0) {
            int points = 14;
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                double rx = x + Math.cos(angle) * 0.8;
                double rz = z + Math.sin(angle) * 0.8;
                level.sendParticles(new DustParticleOptions(0xFF4D4D, 0.9f),
                        rx, y + 0.05, rz, 1, 0, 0, 0, 0);
            }
        }
        // Мягкое золотое свечение вокруг — раз в 15 тиков
        if (player.tickCount % 15 == 0) {
            level.sendParticles(new DustParticleOptions(0xFFD580, 0.8f),
                    x, y + 1.2, z, 2, 0.5, 0.3, 0.5, 0.02);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}