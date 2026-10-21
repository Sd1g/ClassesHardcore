package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.sd1g.classes.network.HudMessagePayload;

import java.util.List;

public class PowerGainAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("power_gain_bonus");

    @Override public String getId() { return "power_gain"; }
    @Override public String getDisplayName() { return "Усиление"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/power_gain"); }

    public static double computeBonus(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int level = player.experienceLevel;
        int levelsPerStep = cfg.getIntEffect("strongman_levels_per_step", 10);
        if (levelsPerStep <= 0) return 0;

        double t1 = cfg.getEffect("strongman_tier1_dmg", 1.0);
        double t2 = cfg.getEffect("strongman_tier2_dmg", 0.5);
        double t3 = cfg.getEffect("strongman_tier3_dmg", 0.25);
        int maxT1 = cfg.getIntEffect("strongman_tier1_max_lvl", 100);
        int maxT2 = cfg.getIntEffect("strongman_tier2_max_lvl", 200);

        double bonusHP = 0;
        bonusHP += (Math.min(level, maxT1) / (double) levelsPerStep) * t1;
        if (level > maxT1) bonusHP += (Math.min(level - maxT1, maxT2 - maxT1) / (double) levelsPerStep) * t2;
        if (level > maxT2) bonusHP += ((level - maxT2) / (double) levelsPerStep) * t3;

        return bonusHP * 2.0; // 1 HP = 2.0 урона
    }

    @Override
    public String getDescription() {
        return "Усиливает удары по мере накопления опыта.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int levelsPerStep = cfg.getIntEffect("strongman_levels_per_step", 10);
        double bonusDamage = computeBonus(player);

        // Расчет уровней до следующего шага (зависит от конфига)
        int toNext = levelsPerStep - (player.experienceLevel % levelsPerStep);

        return List.of(
                "§7Текущий уровень: §f" + player.experienceLevel,
                "§7Текущий бонус: §c+" + String.format("%.2f", bonusDamage / 2.0) + " HP урона",
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        double targetDamage = computeBonus(player);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;

        // Если бонус изменился (игрок получил новый уровень)
        if (Math.abs(oldBonus - targetDamage) > 1e-6) {
            if (targetDamage > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, targetDamage, AttributeModifier.Operation.ADD_VALUE));

                // ЭПИЧНЫЙ ЭФФЕКТ ПРИ УЛУЧШЕНИИ (только если урон вырос, а не упал)
                if (targetDamage > oldBonus + 0.01) {
                    triggerEpicUpgradeEffect(player);
                }
            } else {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }

    private void triggerEpicUpgradeEffect(ServerPlayer player) {
        ServerLevel level = player.level();

        // HUD Уведомление
        ServerPlayNetworking.send(player, new HudMessagePayload("§cСилач: Усиление!", 0xFFFF5555, 80));

        // Триумфальные звуки
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.5f);

        // Мощные визуальные эффекты
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 100, 0.6, 1.2, 0.6, 0.2);
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 50, 1.0, 1.0, 1.0, 0.2);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 60, 1.5, 1.5, 1.5, 0.5);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 40, 0.8, 0.8, 0.8, 0.2);

        // Огненное кольцо по земле
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            level.sendParticles(ParticleTypes.FLAME, x + Math.cos(rad) * 1.5, player.getY() + 0.2, z + Math.sin(rad) * 1.5, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }
}