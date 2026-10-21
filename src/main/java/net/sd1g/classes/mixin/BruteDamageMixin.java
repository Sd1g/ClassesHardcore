package net.sd1g.classes.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class BruteDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float classeshardcore$modifyBruteDamage(float amount, ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;

        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;

        PlayerClassData data = PlayerClassData.get(attacker);
        if (!"brute".equals(data.playerClass)) return amount;

        ModConfig cfg = ModConfig.getInstance();
        ItemStack held = attacker.getMainHandItem();

        if (held.is(Items.MACE)) {
            double mult = cfg.getEffect("brute_mace_damage_multiplier", 1.5);

            // Визуальный эффект тяжелого удара
            if (level.getRandom().nextFloat() < 0.3f) { // Не спамим каждый удар
                level.sendParticles(ParticleTypes.CRIT, ((LivingEntity)(Object)this).getX(), ((LivingEntity)(Object)this).getY() + 1, ((LivingEntity)(Object)this).getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, ((LivingEntity)(Object)this).getX(), ((LivingEntity)(Object)this).getY(), ((LivingEntity)(Object)this).getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.8f);
            }

            return (float) (amount * mult);
        } else {
            double mult = cfg.getEffect("brute_other_damage_multiplier", 0.3);
            return (float) (amount * mult);
        }
    }
}