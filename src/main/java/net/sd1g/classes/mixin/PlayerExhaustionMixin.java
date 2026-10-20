package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public class PlayerExhaustionMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float classeshardcore$reduceExhaustion(float amount) {
        if ((Object) this instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);
            if ("gluttony".equals(data.playerClass)) {
                double mult = ModConfig.getInstance().getEffect("gluttony_hunger_multiplier", 0.5);
                return (float) (amount * mult);
            }
        }
        return amount;
    }
}