package net.sd1g.classes.mixin;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.sd1g.classes.config.ModConfig;
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

        if (!arrow.isCritArrow()) return;

        Entity owner = arrow.getOwner();
        if (owner instanceof ServerPlayer sp) {
            PlayerClassData data = PlayerClassData.get(sp);

            if ("archer".equals(data.playerClass)) {
                long now = sp.level().getGameTime();
                ModConfig cfg = ModConfig.getInstance();

                // Кулдаун из конфига
                if (!data.isOnCooldown("archer_pure_dmg_cd", now)) {
                    Entity target = result.getEntity();
                    if (target instanceof LivingEntity livingTarget) {
                        livingTarget.invulnerableTime = 0;

                        // Урон из конфига
                        float pureDmg = (float) cfg.getEffect("archer_pure_damage", 4.0);
                        PureDamage.apply(sp, livingTarget, pureDmg);

                        // КД из конфига
                        int cdTicks = cfg.getIntEffect("archer_pure_damage_cooldown", 30);
                        data.setCooldown("archer_pure_dmg_cd", now, cdTicks);

                        // КРУТОЙ ЭФФЕКТ ПРОБИВАНИЯ
                        ServerLevel level = (ServerLevel) sp.level();
                        double x = target.getX();
                        double y = target.getY() + (target.getBbHeight() / 2.0);
                        double z = target.getZ();

                        // 1. Голубые искры пробития и криты
                        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 20, 0.3, 0.3, 0.3, 0.1);
                        level.sendParticles(ParticleTypes.CRIT, x, y, z, 15, 0.2, 0.2, 0.2, 0.1);

                        // 2. Зеленые искры (тематика лучника)
                        level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f), x, y, z, 30, 0.4, 0.4, 0.4, 0.1);

                        // 3. Звуки: пробивающий крит и кристальный звон
                        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.5f);
                        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                }
            }
        }
    }
}