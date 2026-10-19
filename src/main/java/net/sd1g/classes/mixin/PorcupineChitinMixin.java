package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Дикобраз — пассивка «Хитиновый покров».
 * Весь входящий урон ×0.8 (умножается на porcupine_chitin_damage_multiplier).
 */
@Mixin(LivingEntity.class)
public class PorcupineChitinMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$chitinReduction(float amount, ServerLevel level, DamageSource source) {
        if (amount <= 0) return amount;
        if (!((Object) this instanceof ServerPlayer sp)) return amount;

        PlayerClassData data = PlayerClassData.get(sp);
        if (!"porcupine".equals(data.playerClass)) return amount;

        double mult = ModConfig.getInstance().getEffect("porcupine_chitin_damage_multiplier", 0.8);
        return (float) (amount * mult);
    }
}