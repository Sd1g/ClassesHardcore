package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class ArcherArrowDamageMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void classeshardcore$applyPureDamage(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        // 1. Проверяем, что лук был натянут полностью (крит)
        if (!arrow.isCritArrow()) return;

        Entity owner = arrow.getOwner();
        if (owner instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);

            // 2. Проверяем класс
            if ("archer".equals(data.playerClass)) {
                long now = sp.level().getGameTime();

                // 3. Проверяем внутренний кулдаун (1.5 секунды = 30 тиков)
                if (!data.isOnCooldown("archer_pure_dmg_cd", now)) {
                    Entity target = result.getEntity();
                    if (target instanceof LivingEntity livingTarget) {

                        // 4. Сбрасываем неуязвимость цели, чтобы чистый урон прошел моментально
                        livingTarget.invulnerableTime = 0;

                        // 5. Наносим ровно 2 HP (4 единицы) чистого урона
                        PureDamage.apply(sp, livingTarget, 4.0f);

                        // 6. Ставим кулдаун на 30 тиков
                        data.setCooldown("archer_pure_dmg_cd", now, 30);
                    }
                }
            }
        }
    }
}