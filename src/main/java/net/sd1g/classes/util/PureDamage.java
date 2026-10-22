package net.sd1g.classes.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.party.PartyManager;

public final class PureDamage {
    public static final ResourceKey<DamageType> PURE_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ClassesHardcore.id("pure_damage")
    );

    private PureDamage() {}

    public static float apply(LivingEntity attacker, LivingEntity target, float amount) {
        if (target == null || amount <= 0) return 0f;

        // Проверка пати (если оба игроки)
        if (attacker instanceof ServerPlayer spA && target instanceof ServerPlayer spT) {
            if (!PartyManager.canHurt(spA, spT)) return 0f;
        }

        if (!(target.level() instanceof ServerLevel level)) return 0f;

        target.invulnerableTime = 0;
        DamageSource source = level.damageSources().source(PURE_DAMAGE_TYPE, attacker);

        float before = target.getHealth() + target.getAbsorptionAmount();
        target.hurtServer(level, source, amount);
        float after = target.getHealth() + target.getAbsorptionAmount();

        return Math.max(0f, before - after);
    }

    public static float apply(ServerPlayer attacker, LivingEntity target, float amount) {
        return apply((LivingEntity) attacker, target, amount);
    }
}