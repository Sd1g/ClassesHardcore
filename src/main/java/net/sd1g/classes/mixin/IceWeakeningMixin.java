package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class IceWeakeningMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$weakenSlowedAttacker(float amount, ServerLevel level, DamageSource source) {
        if (amount <= 0) return amount;

        // 1) Получатель урона должен быть Ледяным воином
        if (!((Object) this instanceof ServerPlayer defender)) return amount;
        PlayerClassData defenderData = PlayerClassData.get(defender);
        if (!"ice_warrior".equals(defenderData.playerClass)) return amount;

        // 2) Атакующий должен быть LivingEntity и не самим защитником
        if (!(source.getEntity() instanceof LivingEntity attacker)) return amount;
        if (attacker == defender) return amount;

        // 3) Атакующий должен быть под Slowness
        MobEffectInstance slowness = attacker.getEffect(MobEffects.SLOWNESS);
        if (slowness == null) return amount;

        int slownessLevel = slowness.getAmplifier() + 1;
        // ИСПРАВЛЕНИЕ: Теперь по умолчанию 0.5 НР за уровень (из документа)
        double perLevel = ModConfig.getInstance().getEffect("ice_weakening_damage_per_level", 0.5);
        float reduction = (float) (slownessLevel * perLevel);

        return Math.max(0f, amount - reduction);
    }
}