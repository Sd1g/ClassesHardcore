package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Увеличивает урон стрел для класса «Лучник».
 * Перехватывает setBaseDamage(double) и добавляет бонус,
 * если владелец стрелы — игрок с классом archer.
 */
@Mixin(AbstractArrow.class)
public class ArcherArrowDamageMixin {

    @ModifyVariable(
            method = "setBaseDamage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private double classeshardcore$boostArrowDamage(double baseDamage) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        Entity owner = arrow.getOwner();
        if (owner instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("archer".equals(data.playerClass)) {
                double bonus = ModConfig.getInstance().getEffect("archer_bow_damage_bonus", 3.0);
                return baseDamage + bonus;
            }
        }
        return baseDamage;
    }
}