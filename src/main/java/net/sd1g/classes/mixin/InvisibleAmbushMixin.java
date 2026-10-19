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

/**
 * «Невидимка»: первый удар из невидимости наносит увеличенный урон.
 * Не применяется к булаве. Кристаллы Энда отсеиваются автоматически,
 * потому что source.getEntity() для них не игрок.
 */
@Mixin(LivingEntity.class)
public class InvisibleAmbushMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$ambushBonus(float amount, ServerLevel level, DamageSource source) {
        if (amount <= 0) return amount;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;

        PlayerClassData data = PlayerClassData.get(attacker);
        if (!"invisible".equals(data.playerClass)) return amount;
        if (!data.invisibleAmbushArmed) return amount;

        // Исключение: булава
        if (attacker.getMainHandItem().is(Items.MACE)) return amount;

        double mult = ModConfig.getInstance().getEffect("ambush_damage_multiplier", 2.2);
        // Флаг и эффект снимаются в InvisibilityAbility.onDealDamage (AFTER_DAMAGE)
        return (float) (amount * mult);
    }
}