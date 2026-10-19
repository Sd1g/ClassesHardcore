package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранит пары «UUID кристалла Энда → UUID игрока, который ударил его
 * (и тем самым спровоцировал взрыв)». Используется при атрибуции урона
 * от взрыва кристалла.
 */
public class EndCrystalTracker {

    private static final Map<UUID, UUID> CRYSTAL_TO_ATTACKER = new HashMap<>();

    public static void register(UUID crystalUuid, UUID attackerUuid) {
        CRYSTAL_TO_ATTACKER.put(crystalUuid, attackerUuid);
    }

    public static ServerPlayer getAttacker(UUID crystalUuid, ServerLevel level) {
        UUID attackerUuid = CRYSTAL_TO_ATTACKER.get(crystalUuid);
        if (attackerUuid == null) return null;
        if (level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(attackerUuid);
    }

    public static void remove(UUID crystalUuid) {
        CRYSTAL_TO_ATTACKER.remove(crystalUuid);
    }
}