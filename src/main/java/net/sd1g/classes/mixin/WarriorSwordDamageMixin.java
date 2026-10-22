package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class WarriorSwordDamageMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float classeshardcore$swordDamage(float amount, ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;
        if (!"warrior".equals(PlayerClassData.get(attacker).playerClass)) return amount;

        if (attacker.getMainHandItem().is(ItemTags.SWORDS)) {
            double percent = ModConfig.getInstance().get("warrior", "warrior_combat_experience.sword_bonus_percent", 15.0);
            return (float) (amount * (1.0 + (percent / 100.0)));
        }
        return amount;
    }
}