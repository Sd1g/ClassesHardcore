package net.sd1g.classes.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.CooldownSyncPayload;

public class AbilityManager {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;

            for (ServerPlayer player : players) {
                PlayerClassData data = PlayerClassData.get(player);
                ClassDefinition def = data.getClassDef();
                if (def == null || def.abilities.isEmpty()) continue;

                int tick = player.tickCount;
                for (Ability ability : def.abilities) {
                    int interval = ability.getTickInterval();
                    if (interval <= 1 || tick % interval == 0) {
                        ability.onPassiveTick(player);
                    }
                }
            }
        });
    }

    public static boolean tryActivate(ServerPlayer player, String abilityId) {
        PlayerClassData data = PlayerClassData.get(player);
        ClassDefinition def = data.getClassDef();
        if (def == null) return false;

        Ability ability = null;
        for (Ability a : def.abilities) {
            if (a.getId().equals(abilityId) && !a.isPassive()) {
                ability = a;
                break;
            }
        }
        if (ability == null) return false;

        long currentTick = player.level().getServer().getTickCount();
        if (data.isOnCooldown(abilityId, currentTick)) return false;

        boolean success = ability.onActivate(player);
        if (!success) return false;

        int cd;
        // ИСПРАВЛЕНИЕ: Для телепорта всегда используем динамический расчет (мин, макс, за блок)
        if ("teleport".equals(abilityId)) {
            cd = ability.getDynamicCooldown(player);
        } else {
            int defaultCd = ability.getDynamicCooldown(player);
            cd = ModConfig.getInstance().getCooldown(abilityId, defaultCd);
        }

        data.setCooldown(abilityId, currentTick, cd);
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new CooldownSyncPayload(abilityId, cd));
        return true;
    }
}