package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class InvisibleAmbushMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float classeshardcore$ambushBonus(float amount, ServerLevel level, DamageSource source) {
        if (amount <= 0) return amount;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;

        PlayerClassData data = PlayerClassData.get(attacker);
        if (!"invisible".equals(data.playerClass) || !data.invisibleAmbushArmed) return amount;
        if (attacker.getMainHandItem().is(Items.MACE)) return amount;

        double mult = ModConfig.getInstance().getEffect("ambush_damage_multiplier", 3.0);
        return (float) (amount * mult);
    }
}