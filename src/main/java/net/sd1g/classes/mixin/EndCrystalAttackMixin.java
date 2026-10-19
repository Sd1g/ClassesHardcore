package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.sd1g.classes.ability.impl.EndCrystalTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ловим момент, когда кристалл Энда получает урон. Если атакующий —
 * ServerPlayer, запоминаем связку (UUID кристалла → UUID атакующего).
 * Позже {@link ExplosionDamageMixin} применит классовую скидку урона
 * именно для атакующего.
 *
 * ВАЖНО: 26.2 объявляет кристалл как Entity (не LivingEntity),
 * поэтому миксин на hurt(DamageSource, float), а не на hurtServer.
 */
@Mixin(EndCrystal.class)
public class EndCrystalAttackMixin {

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            require = 0
    )
    private void classeshardcore$trackCrystalAttacker(DamageSource source,
                                                      float amount,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer sp)) return;

        EndCrystal crystal = (EndCrystal) (Object) this;
        EndCrystalTracker.register(crystal.getUUID(), sp.getUUID());
    }
}