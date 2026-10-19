package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ускоряет натяжение лука для класса «Лучник».
 * Перехватывает вызов BowItem.getPowerForTime(int) в releaseUsing
 * и подаёт на вход увеличенное значение useTime.
 */
@Mixin(BowItem.class)
public class ArcherBowDrawMixin {

    @Redirect(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"
            )
    )
    private float classeshardcore$boostDrawSpeed(int useTime,
                                                 ItemStack stack,
                                                 Level level,
                                                 LivingEntity entity,
                                                 int timeLeft) {
        int modified = useTime;
        if (entity instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("archer".equals(data.playerClass)) {
                double mult = ModConfig.getInstance().getEffect("archer_draw_speed_multiplier", 1.5);
                modified = (int) (useTime * mult);
            }
        }
        return BowItem.getPowerForTime(modified);
    }
}