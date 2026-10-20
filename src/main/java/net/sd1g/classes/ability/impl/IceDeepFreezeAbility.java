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

import java.util.List;

public class IceDeepFreezeAbility implements Ability {
    @Override public String getId() { return "ice_deep_freeze"; }
    @Override public String getDisplayName() { return "Глубокая заморозка"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_deep_freeze"); }

    @Override
    public String getDescription() {
        return "С вероятностью 3% удар полностью останавливает противника на 2,5 секунды.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §a3%", "§7Обездвиживание: §b2.5 сек.");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (attacker.getRandom().nextDouble() >= 0.03) return;

        int freezeTicks = 50; // 2.5 сек
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeTicks, 9, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, freezeTicks, 9, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.5, z = target.getZ();

        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 40, 0.8, 1.0, 0.8, 0.15);
        level.sendParticles(new DustParticleOptions(0x00C8FF, 2.0f), x, y, z, 30, 0.7, 0.9, 0.7, 0.15);
        level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6f, 0.6f);

        attacker.sendOverlayMessage(Component.literal("§bГлубокая заморозка!"));
    }
}