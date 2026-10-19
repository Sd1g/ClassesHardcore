package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Многоуровневый трекер виновников для ТНТ:
 *  1. RECENT_IGNITIONS — поджиги зажигалкой/огненным шаром/огненной стрелой.
 *  2. PLACEMENTS — установки TNT-блока или redstone-компонента (факел,
 *     рычаг, кнопка, плита, redstone-блок) и нажатия рычагов/кнопок.
 *  3. TNT_TO_OWNER — привязка конкретного PrimedTnt к UUID виновника.
 *
 * При появлении PrimedTnt:
 *   - Сначала ищем свежий поджиг (наивысший приоритет).
 *   - Затем ищем САМОЕ СВЕЖЕЕ размещение рядом с ТНТ (кто последний
 *     настраивал механизм — тот и виновник).
 */
public class TntOwnerTracker {

    private record Ignition(BlockPos pos, UUID playerUuid, long tick) {}
    private record Placement(UUID playerUuid, long tick) {}

    private static final List<Ignition> RECENT_IGNITIONS = new ArrayList<>();
    private static final Map<BlockPos, Placement> PLACEMENTS = new HashMap<>();
    private static final Map<UUID, UUID> TNT_TO_OWNER = new HashMap<>();

    private static final int IGNITION_TTL = 20;              // 1 сек
    private static final int PLACEMENT_TTL = 20 * 60 * 30;   // 30 мин
    private static final double IGNITION_RADIUS_SQ = 9.0;    // 3 блока
    private static final double PLACEMENT_RADIUS_SQ = 16.0;  // 4 блока

    public static void recordIgnition(BlockPos pos, UUID playerUuid, long tick) {
        RECENT_IGNITIONS.add(new Ignition(pos.immutable(), playerUuid, tick));
    }

    public static void recordPlacement(BlockPos pos, UUID playerUuid, long tick) {
        PLACEMENTS.put(pos.immutable(), new Placement(playerUuid, tick));
    }

    public static void bindTntToIgniter(UUID tntUuid, BlockPos tntPos, long now, ServerLevel level) {
        // 1) Явный поджиг — приоритет
        RECENT_IGNITIONS.removeIf(i -> now - i.tick() > IGNITION_TTL);
        UUID fromIgnition = null;
        long bestIgnitionTick = -1;
        for (Ignition i : RECENT_IGNITIONS) {
            if (i.pos().distSqr(tntPos) > IGNITION_RADIUS_SQ) continue;
            if (i.tick() > bestIgnitionTick) {
                bestIgnitionTick = i.tick();
                fromIgnition = i.playerUuid();
            }
        }
        if (fromIgnition != null) {
            TNT_TO_OWNER.put(tntUuid, fromIgnition);
            final BlockPos tntFinal = tntPos;
            RECENT_IGNITIONS.removeIf(i -> i.pos().distSqr(tntFinal) <= IGNITION_RADIUS_SQ);
            PLACEMENTS.entrySet().removeIf(e -> e.getKey().distSqr(tntFinal) <= PLACEMENT_RADIUS_SQ);
            return;
        }

        // 2) Самое свежее размещение рядом с ТНТ
        UUID fromPlacement = null;
        long bestPlacementTick = -1;
        BlockPos bestKey = null;
        for (Map.Entry<BlockPos, Placement> e : PLACEMENTS.entrySet()) {
            if (e.getKey().distSqr(tntPos) > PLACEMENT_RADIUS_SQ) continue;
            if (e.getValue().tick() > bestPlacementTick) {
                bestPlacementTick = e.getValue().tick();
                fromPlacement = e.getValue().playerUuid();
                bestKey = e.getKey();
            }
        }
        if (fromPlacement != null) {
            TNT_TO_OWNER.put(tntUuid, fromPlacement);
            if (bestKey != null) PLACEMENTS.remove(bestKey);
        }
    }

    public static ServerPlayer getOwner(UUID tntUuid, ServerLevel level) {
        UUID ownerUuid = TNT_TO_OWNER.remove(tntUuid);
        if (ownerUuid == null) return null;
        if (level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public static void cleanupOld(long now) {
        RECENT_IGNITIONS.removeIf(i -> now - i.tick() > IGNITION_TTL);
        Iterator<Map.Entry<BlockPos, Placement>> it = PLACEMENTS.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().tick() > PLACEMENT_TTL) it.remove();
        }
    }
}