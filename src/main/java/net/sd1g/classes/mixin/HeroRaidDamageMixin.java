package net.sd1g.classes.mixin;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raider;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class HeroRaidDamageMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float classeshardcore$raidDamage(float amount, ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;
        if (!"hero".equals(PlayerClassData.get(attacker).playerClass)) return amount;

        if ((Object) this instanceof Raider) {
            double mult = ModConfig.getInstance().getEffect("hero_raid_damage_multiplier", 3.0);

            double x = ((LivingEntity)(Object)this).getX();
            double y = ((LivingEntity)(Object)this).getY() + (((LivingEntity)(Object)this).getBbHeight() / 2.0);
            double z = ((LivingEntity)(Object)this).getZ();

            // ЭПИЧЕСКИЙ УДАР КОЛОССАЛЬНОЙ СИЛЫ
            // 1. Взрыв и ударная волна
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.5, 0.5, 0.5, 0.2);

            // 2. Золотой свет героя (Тотем + Золотая пыль)
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.6, 0.6, 0.6, 0.2);
            level.sendParticles(new DustParticleOptions(0xFFD700, 2.5f), x, y, z, 30, 0.8, 0.8, 0.8, 0.15);

            // 3. Звуки удара
            // Тяжелый удар булавы об землю + звон колокола (святость и мощь)
            level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.playSound(null, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);

            return (float) (amount * mult);
        }
        return amount;
    }
}