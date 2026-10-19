package net.sd1g.classes.ability.impl;

import java.util.UUID;

/**
 * Хранит игрока, который последним попытался «поспать» в кровати,
 * взрывающейся в Нижнем мире/Энде. Используется как атрибуция при взрыве кровати.
 */
public class BedExplosionTracker {
    private static UUID recentPlacer = null;
    private static long recentTick = -1;

    public static void register(UUID playerUuid, long tick) {
        recentPlacer = playerUuid;
        recentTick = tick;
    }

    public static UUID getRecentPlacer(long now, int ttlTicks) {
        if (recentPlacer == null) return null;
        if (now - recentTick > ttlTicks) return null;
        return recentPlacer;
    }

    public static void clear() {
        recentPlacer = null;
        recentTick = -1;
    }
}