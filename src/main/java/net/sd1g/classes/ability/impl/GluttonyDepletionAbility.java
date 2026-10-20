package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class GluttonyDepletionAbility implements Ability {
    @Override public String getId() { return "depletion"; }
    @Override public String getDisplayName() { return "Истощение"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1000; } // 50 сек

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/depletion"); }

    @Override
    public String getDescription() {
        return "Следующий удар накладывает Голод V на 20 секунд. Перезарядка: 50 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект: §6Голод V", "§7Длительность: §f20 сек.");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (!data.gluttonyDepletionArmed) return;
        if (player.tickCount % 10 == 0) {
            ServerLevel level = player.level();
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.1f), player.getX() + 0.4, player.getY() + 1.1, player.getZ(), 2, 0.15, 0.1, 0.15, 0.02);
        }
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.gluttonyDepletionArmed = true;
        PlayerClassData.markDirty(player);

        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.6, 0.6, 0.6, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.3f, 0.8f);
        player.sendOverlayMessage(Component.literal("§4Истощение активировано. §fСледующий удар — Голод."));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.gluttonyDepletionArmed) return;
        data.gluttonyDepletionArmed = false;
        PlayerClassData.markDirty(attacker);

        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 4, false, true)); // 20 сек, Голод V

        ServerLevel level = attacker.level();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }
}