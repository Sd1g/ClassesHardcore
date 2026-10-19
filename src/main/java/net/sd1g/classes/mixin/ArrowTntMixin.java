package net.sd1g.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.sd1g.classes.ability.impl.TntOwnerTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Когда горящая стрела попадает в блок TNT — записываем стрелка
 * как виновника поджига. Покрывает способ активации через огненный лук.
 */
@Mixin(AbstractArrow.class)
public class ArrowTntMixin {

    @Inject(method = "onHitBlock", at = @At("HEAD"), require = 0)
    private void classeshardcore$trackArrowHit(BlockHitResult hitResult, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!self.isOnFire()) return;

        Entity owner = self.getOwner();
        if (!(owner instanceof ServerPlayer sp)) return;
        if (!(self.level() instanceof ServerLevel level)) return;

        BlockPos pos = hitResult.getBlockPos();
        if (!level.getBlockState(pos).is(Blocks.TNT)) return;

        long tick = level.getServer() != null ? level.getServer().getTickCount() : 0;
        TntOwnerTracker.recordIgnition(pos, sp.getUUID(), tick);
    }
}