package net.sd1g.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sd1g.classes.ability.impl.BedExplosionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ловим момент, когда игрок пытается лечь в кровать в Нижнем мире или Энде,
 * где это приведёт к взрыву. Регистрируем его UUID в трекере, чтобы потом
 * {@link ExplosionDamageMixin} мог применить классовую скидку урона именно
 * к виновнику взрыва.
 */
@Mixin(BedBlock.class)
public class BedUseMixin {

    @Inject(
            method = "useWithoutBlock",
            at = @At("HEAD"),
            require = 0
    )
    private void classeshardcore$trackBedUse(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit,
                                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer sp)) return;

        ResourceKey<Level> dim = level.dimension();
        boolean bedExplodes = dim == Level.NETHER || dim == Level.END;
        if (!bedExplodes) return;

        long tick = level.getServer() != null ? level.getServer().getTickCount() : 0;
        BedExplosionTracker.register(sp.getUUID(), tick);
    }
}