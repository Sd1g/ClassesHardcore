package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.VampireSyncPayload;

import java.util.List;

public class VampireDrainAbility implements Ability {
    @Override public String getId() { return "vampire_drain"; }
    @Override public String getDisplayName() { return "Вампиризм"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1200; } // 60 секунд

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/vampire_drain"); }

    @Override
    public String getDescription() {
        return "На 10 секунд удары лечат на 70% фактически нанесённого урона. Перезарядка: 60 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of(
                "§7Длительность: §f10 сек.",
                "§7Лечение: §a70% §7от урона",
                "§7Кулдаун: §f60 сек."
        );
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.vampireActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.vampireActiveTicks = 200; // 10 секунд
        PlayerClassData.markDirty(player);
        ServerPlayNetworking.send(player, new VampireSyncPayload(200));

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.4f, 0.8f);
        player.sendOverlayMessage(Component.literal("§cВампиризм активирован!"));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks <= 0) return;

        data.vampireActiveTicks--;

        if (data.vampireActiveTicks <= 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));
            player.sendOverlayMessage(Component.literal("§cВампиризм закончился"));
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks <= 0) return;

        attacker.heal(damageDealt * 0.70f);

        ServerLevel level = attacker.level();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks > 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));
        }
    }
}