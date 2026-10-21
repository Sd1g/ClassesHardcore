package net.sd1g.classes.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class StuntmanFallDamageMixin {

    @ModifyVariable(
            method = "causeFallDamage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$reduceFallDamageMultiplier(float multiplier) {
        if ((Object) this instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("stuntman".equals(data.playerClass)) {
                double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.5);

                // Если урон от падения должен был быть (проверяем высоту)
                if (sp.fallDistance > 3.0F) {
                    ServerLevel level = sp.level();
                    double x = sp.getX();
                    double y = sp.getY() + 0.1;
                    double z = sp.getZ();

                    // Много частиц слизи
                    level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 50, 0.5, 0.1, 0.5, 0.1);
                    level.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.2, z, 20, 0.3, 0.1, 0.3, 0.05);

                    // Звуки: шлепок слизи + звук тяжелого падения
                    level.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1.5f, 0.8f);
                    level.playSound(null, x, y, z, SoundEvents.GENERIC_BIG_FALL, SoundSource.PLAYERS, 1.0f, 0.8f);
                }

                return (float) (multiplier * mult);
            }
        }
        return multiplier;
    }
}