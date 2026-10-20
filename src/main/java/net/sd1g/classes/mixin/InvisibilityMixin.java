package net.sd1g.classes.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class) // <-- Изменено с LivingEntity на Entity
public class InvisibilityMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void classeshardcore$isInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof net.minecraft.server.level.ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("invisible".equals(data.playerClass) && data.invisibilityActiveTicks > 0) {
                cir.setReturnValue(true); // Скрывает модель, броню и предметы
            }
        }
    }
}