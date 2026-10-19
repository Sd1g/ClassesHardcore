package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Модифицирует урон, наносимый игроком с классом «Безумный фермер».
 * <ul>
 *   <li>Мотыга — урон ×2 (+100%) + накопительный бонус</li>
 *   <li>Любое другое оружие — урон ×0.2 (−80%)</li>
 * </ul>
 * Взрывы (ТНТ, кровать, кристаллы Энда) пропускаются — их обрабатывает
 * {@link ExplosionDamageMixin}, чтобы штраф не применялся дважды.
 */
@Mixin(LivingEntity.class)
public class FarmerDamageMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$modifyFarmerDamage(float amount, ServerLevel level, DamageSource source) {
        // Взрывы обрабатываются отдельным ExplosionDamageMixin
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;

        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;

        PlayerClassData data = PlayerClassData.get(attacker);
        if (!"mad_farmer".equals(data.playerClass)) return amount;

        ModConfig cfg = ModConfig.getInstance();
        ItemStack held = attacker.getMainHandItem();

        if (held.is(ItemTags.HOES)) {
            double mult = cfg.getEffect("farmer_hoe_damage_multiplier", 2.0);
            double mobsPerStep = cfg.getEffect("farmer_mobs_per_bonus_point", 100.0);
            double bonusPerStep = cfg.getEffect("farmer_bonus_per_mob_step", 0.25);
            double bonusPerPlayer = cfg.getEffect("farmer_bonus_per_player_kill", 5.0);

            int step = mobsPerStep > 0 ? (int) (data.farmerMobKills / mobsPerStep) : 0;
            double accumulatedBonus = step * bonusPerStep + data.farmerPlayerKills * bonusPerPlayer;

            return (float) (amount * mult + accumulatedBonus);
        } else {
            double mult = cfg.getEffect("farmer_other_damage_multiplier", 0.2);
            return (float) (amount * mult);
        }
    }
}