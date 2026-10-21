package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.sd1g.classes.network.HudMessagePayload;

import java.util.List;

public class LifeExperienceAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("life_experience_bonus_hp");

    @Override public String getId() { return "life_experience"; }
    @Override public String getDisplayName() { return "Жизненный опыт"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/life_experience"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int per = Math.max(1, cfg.getIntEffect("life_exp_levels_per_point", 5));
        double hp = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);
        return "За каждые " + per + " уровней опыта +" + hp + " НР к максимуму.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int per = Math.max(1, cfg.getIntEffect("life_exp_levels_per_point", 5));
        double hpPerPoint = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);

        int level = player.experienceLevel;
        int points = level / per;
        double bonus = points * hpPerPoint;
        int toNext = per - (level % per);

        return List.of(
                "§7Текущий бонус: §c+" + bonus + " HP",
                "§7До следующего шага: §f" + toNext + " ур."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int per = Math.max(1, cfg.getIntEffect("life_exp_levels_per_point", 5));
        double hpPerPoint = cfg.getEffect("life_exp_bonus_hp_per_point", 1.0);

        int points = player.experienceLevel / per;
        double targetBonus = points * hpPerPoint;

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        double oldBonus = existing != null ? existing.amount() : 0.0;

        // Если бонус изменился (игрок получил новый уровень)
        if (Math.abs(oldBonus - targetBonus) > 1e-6) {
            if (targetBonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, targetBonus, AttributeModifier.Operation.ADD_VALUE));

                // ЭПИЧНЫЙ ЭФФЕКТ ПРИ ПРОКАЧКЕ (только если здоровье выросло)
                if (targetBonus > oldBonus + 0.01) {
                    triggerEpicUpgradeEffect(player);
                }
            } else if (existing != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }

    private void triggerEpicUpgradeEffect(ServerPlayer player) {
        ServerLevel level = player.level();

        // HUD Уведомление
        ServerPlayNetworking.send(player, new HudMessagePayload("§cЖивчик: Здоровье увеличено!", 0xFFFF5555, 80));

        // Эпичные звуки
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);

        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // 1. Много сердец и золотых искр (здоровье и жизнь)
        level.sendParticles(ParticleTypes.HEART, x, y, z, 30, 0.6, 1.0, 0.6, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.5, 1.0, 0.5, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.8, 1.2, 0.8, 0.1);

        // 2. Красно-розово-золотая магия (DustParticleOptions)
        level.sendParticles(new DustParticleOptions(0xFF0000, 2.5f), x, y, z, 40, 0.8, 1.0, 0.8, 0.2); // Красный
        level.sendParticles(new DustParticleOptions(0xFF69B4, 2.0f), x, y, z, 30, 0.6, 0.8, 0.6, 0.1); // Розовый
        level.sendParticles(new DustParticleOptions(0xFFD700, 1.5f), x, y, z, 30, 1.0, 1.0, 1.0, 0.2); // Золотой

        // 3. Эпичное кольцо по земле
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            level.sendParticles(new DustParticleOptions(0xFFD700, 2.0f),
                    x + Math.cos(rad) * 2.5, player.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(new DustParticleOptions(0xFF0000, 1.5f),
                    x + Math.cos(rad) * 1.5, player.getY() + 0.2, z + Math.sin(rad) * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }
}