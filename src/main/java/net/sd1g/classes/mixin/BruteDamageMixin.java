package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * Модифицирует урон, наносимый игроком с классом «Громила».
 * <ul>
 *   <li>Булава — урон ×1.5 (+50%)</li>
 *   <li>Любое другое оружие — урон ×0.3 (−70%)</li>
 * </ul>
 * Взрывы (ТНТ, кровать, кристаллы Энда) пропускаются — их обрабатывает
 * {@link ExplosionDamageMixin}, чтобы штраф не применялся дважды.
 */
@Mixin(LivingEntity.class)
public class BruteDamageMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$modifyBruteDamage(float amount, ServerLevel level, DamageSource source) {
        // Взрывы обрабатываются отдельным ExplosionDamageMixin
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;

        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;

        PlayerClassData data = PlayerClassData.get(attacker);
        if (!"brute".equals(data.playerClass)) return amount;

        ModConfig cfg = ModConfig.getInstance();
        ItemStack held = attacker.getMainHandItem();

        if (held.is(Items.MACE)) {
            double mult = cfg.getEffect("brute_mace_damage_multiplier", 1.5);
            return (float) (amount * mult);
        } else {
            double mult = cfg.getEffect("brute_other_damage_multiplier", 0.3);
            return (float) (amount * mult);
        }
    }
}