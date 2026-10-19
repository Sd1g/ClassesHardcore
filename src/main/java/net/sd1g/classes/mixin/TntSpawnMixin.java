package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.sd1g.classes.ability.impl.TntOwnerTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class TntSpawnMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void classeshardcore$trackSpawnedTnt(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PrimedTnt tnt)) return;
        ServerLevel level = (ServerLevel) (Object) this;
        long tick = level.getServer() != null ? level.getServer().getTickCount() : 0;
        TntOwnerTracker.bindTntToIgniter(tnt.getUUID(), tnt.blockPosition(), tick, level);
    }
}