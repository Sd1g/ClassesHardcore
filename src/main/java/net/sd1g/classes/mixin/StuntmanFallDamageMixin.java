package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Уменьшает урон от падения для класса «Каскадер».
 * Перехватывает множитель урона (damageModifier) в LivingEntity.causeFallDamage.
 *
 * Сигнатура метода: causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource)
 * damageModifier — единственный float-аргумент, поэтому ordinal = 0.
 */
@Mixin(LivingEntity.class)
public class StuntmanFallDamageMixin {

    @ModifyVariable(
            method = "causeFallDamage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$reduceFallDamageMultiplier(float multiplier) {
        // Применяем уменьшение только для серверных игроков
        if ((Object) this instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("stuntman".equals(data.playerClass)) {
                double mult = ModConfig.getInstance().getEffect("stuntman_fall_damage_multiplier", 0.5);
                return (float) (multiplier * mult);
            }
        }
        return multiplier;
    }
}