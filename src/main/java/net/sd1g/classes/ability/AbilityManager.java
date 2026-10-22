package net.sd1g.classes.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.network.CooldownSyncPayload;
import net.sd1g.classes.util.AttributesHelper;

public class AbilityManager {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;

            for (ServerPlayer player : players) {
                PlayerClassData data = PlayerClassData.get(player);

                if (data.isCasting) {
                    if (player.isDeadOrDying()) {
                        stopCasting(player, data);
                    } else {
                        // АБСОЛЮТНАЯ БЛОКИРОВКА
                        // Используем простую телепортацию на координаты каста
                        player.teleportTo(data.castX, data.castY, data.castZ);
                        player.setDeltaMovement(Vec3.ZERO);
                        player.hurtMarked = true;
                        player.resetFallDistance();
                        if (player.isSprinting()) player.setSprinting(false);

                        data.castTicksRemaining--;
                        if (data.castTicksRemaining <= 0) {
                            Ability ability = getAbilityById(data.castingAbilityId, data);
                            if (ability != null) {
                                executeAbility(player, data, ability);
                            }
                            stopCasting(player, data);
                        }
                    }
                    continue;
                }

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
        if (data.isCasting) return false;

        ClassDefinition def = data.getClassDef();
        if (def == null) return false;

        Ability ability = getAbilityById(abilityId, data);
        if (ability == null || ability.isPassive()) return false;

        long currentTick = player.level().getGameTime();
        if (data.isOnCooldown(abilityId, currentTick)) return false;

        int castTime = ability.getCastTimeTicks(player);

        if (castTime > 0) {
            data.isCasting = true;
            data.castingAbilityId = abilityId;
            data.castTicksRemaining = castTime;
            data.totalCastTicks = castTime;

            data.castX = player.getX();
            data.castY = player.getY();
            data.castZ = player.getZ();

            // Замораживаем скорость на 100%
            AttributesHelper.add(player, Attributes.MOVEMENT_SPEED, "casting_freeze", -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;

            ServerPlayNetworking.send(player, new ActiveTimerPayload("casting_" + abilityId, castTime));
            return true;
        } else {
            return executeAbility(player, data, ability);
        }
    }

    private static void stopCasting(ServerPlayer player, PlayerClassData data) {
        data.isCasting = false;
        data.castingAbilityId = "";
        AttributesHelper.remove(player, Attributes.MOVEMENT_SPEED, "casting_freeze");
    }

    private static boolean executeAbility(ServerPlayer player, PlayerClassData data, Ability ability) {
        boolean success = ability.onActivate(player);
        if (!success) return false;

        long currentTick = player.level().getGameTime();
        int cd = ability.getCooldownTicks(player);
        data.setCooldown(ability.getId(), currentTick, cd);
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new CooldownSyncPayload(ability.getId(), cd, cd));
        return true;
    }

    private static Ability getAbilityById(String id, PlayerClassData data) {
        ClassDefinition def = data.getClassDef();
        if (def != null) {
            for (Ability a : def.abilities) {
                if (a.getId().equals(id)) return a;
            }
        }
        return null;
    }
}