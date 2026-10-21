package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;

import java.util.List;

public class CombatTempoAbility implements Ability {
    private static final Identifier MODIFIER_ID = ClassesHardcore.id("combat_tempo_buff");

    @Override public String getId() { return "combat_tempo"; }
    @Override public String getDisplayName() { return "Боевой темп"; }
    @Override public boolean isPassive() { return false; }

    // ДОБАВЛЕНА ИКОНКА
    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_tempo"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("warrior_tempo_cooldown", 700);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("warrior_tempo_duration", 160) / 20;
        double mult = 1.0 + cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);
        int cd = cfg.getCooldown("warrior_tempo_cooldown", 700) / 20;
        return "На " + dur + " сек. ускоряет восстановление атаки в " + mult + " раза. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("warrior_tempo_duration", 160);
        double mult = 1.0 + cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);
        return List.of("§7Эффект: §bСкорость атаки x" + mult + " §7на §f" + (dur / 20) + " сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int duration = cfg.getIntEffect("warrior_tempo_duration", 160);
        double bonus = cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);

        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            PlayerClassData data = PlayerClassData.get(player);
            data.setCooldown("combat_tempo_active", player.level().getGameTime(), duration);
        }

        ServerPlayNetworking.send(player, new ActiveTimerPayload("combat_tempo", duration));

        ServerLevel level = player.level();
        // Стартовый взрыв огня
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, 0.5, 0.5, 0.1);

        // Звук ярости при активации
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.2f);

        player.sendOverlayMessage(Component.literal("§eБоевой темп!"));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (data.isOnCooldown("combat_tempo_active", now)) {
            // Способность активна
            ServerLevel level = player.level();

            // Огненный вихрь вокруг персонажа
            if (player.tickCount % 2 == 0) {
                double radius = 1.0;
                double angle = (now * 0.5) % (Math.PI * 2);
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + 0.5 + (Math.sin(now * 0.5) * 0.5);

                level.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
                level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
            }

            // Звуки ярости/огня во время действия (раз в 10 тиков)
            if (player.tickCount % 10 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        } else {
            // Способность неактивна - снимаем бафф
            AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attr != null && attr.getModifier(MODIFIER_ID) != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }
}