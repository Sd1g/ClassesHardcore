package net.sd1g.classes.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.party.PartyManager;

public final class NormalDamage {
    private NormalDamage() {}

    public static float apply(ServerPlayer attacker, LivingEntity target, float amount) {
        if (target == null || amount <= 0) return 0f;

        // Проверка пати
        if (target instanceof ServerPlayer spT) {
            if (!PartyManager.canHurt(attacker, spT)) return 0f;
        }

        if (!(target.level() instanceof ServerLevel level)) return 0f;

        target.invulnerableTime = 0;
        DamageSource source = level.damageSources().playerAttack(attacker);

        float before = target.getHealth() + target.getAbsorptionAmount();
        target.hurtServer(level, source, amount);
        float after = target.getHealth() + target.getAbsorptionAmount();

        return Math.max(0f, before - after);
    }
}