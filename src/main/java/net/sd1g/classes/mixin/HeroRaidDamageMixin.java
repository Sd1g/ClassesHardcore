package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            return (float) (amount * mult);
        }
        return amount;
    }
}