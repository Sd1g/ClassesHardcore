package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.util.AttributesHelper;

public class WarriorCombatTempoAbility implements Ability {
    @Override public String getId() { return "warrior_combat_tempo"; }
    @Override public String getDisplayName() { return "Боевой темп"; }
    @Override public boolean isPassive() { return false; }
    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_tempo"); }

    @Override
    public int getBaseCooldownTicks() {
        return ModConfig.getInstance().getTicks("warrior", "warrior_combat_tempo.cooldown_ticks", 700);
    }

    @Override
    public TooltipData buildTooltip(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getTicks("warrior", "warrior_combat_tempo.duration_ticks", 160);
        double bonus = cfg.get("warrior", "warrior_combat_tempo.attack_speed_bonus", 0.5);

        return TooltipData.builder("Ускоряет скорость атаки в 1.5 раза.")
                .cooldown(getBaseCooldownTicks())
                .duration(dur)
                .extra("§7Скорость атаки: §b+" + (int)(bonus * 100) + "%")
                .build();
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int duration = cfg.getTicks("warrior", "warrior_combat_tempo.duration_ticks", 160);
        double bonus = cfg.get("warrior", "warrior_combat_tempo.attack_speed_bonus", 0.5);

        AttributesHelper.add(player, Attributes.ATTACK_SPEED, "warrior_combat_tempo_buff", bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        PlayerClassData data = PlayerClassData.get(player);
        data.setCooldown("warrior_tempo_active", player.level().getGameTime(), duration);

        ServerPlayNetworking.send(player, new ActiveTimerPayload("warrior_combat_tempo_buff", duration));

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.2f);

        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (!data.isOnCooldown("warrior_tempo_active", now)) {
            AttributesHelper.remove(player, Attributes.ATTACK_SPEED, "warrior_combat_tempo_buff");
        }
    }
}