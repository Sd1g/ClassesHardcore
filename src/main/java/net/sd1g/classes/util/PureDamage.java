package net.sd1g.classes.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

public class PureDamage {
    public static void apply(ServerPlayer attacker, LivingEntity target, float amount) {
        if (amount <= 0) return;

        // Создаем магический источник урона (обходит броню)
        DamageSource source = target.damageSources().source(DamageTypes.MAGIC, attacker);

        // ИСПРАВЛЕНИЕ: Используем hurtServer вместо устаревшего hurt
        if (target.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, source, amount);
        }
    }
}