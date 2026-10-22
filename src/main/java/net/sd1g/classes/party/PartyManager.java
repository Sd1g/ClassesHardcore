package net.sd1g.classes.party;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.PartySyncPayload;

import java.util.*;

public class PartyManager {
    private static final Map<UUID, UUID> PENDING_INVITES = new HashMap<>();

    public static Party getParty(MinecraftServer server, UUID playerUuid) {
        for (Party p : PartyStateSaver.get(server).parties) {
            if (p.members.contains(playerUuid)) return p;
        }
        return null;
    }

    public static boolean canHurt(ServerPlayer attacker, ServerPlayer target) {
        if (attacker == target) return true;
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.getGlobalBool("party.enabled", true)) return true;
        if (cfg.getGlobalBool("party.allow_friendly_fire", false)) return true;

        Party p = getParty(attacker.level().getServer(), attacker.getUUID());
        return p == null || !p.members.contains(target.getUUID());
    }

    public static boolean canApplyNegativeEffect(ServerPlayer attacker, ServerPlayer target) {
        if (attacker == target) return true;
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.getGlobalBool("party.enabled", true)) return true;
        if (cfg.getGlobalBool("party.negative_abilities_affect_allies", false)) return true;

        Party p = getParty(attacker.level().getServer(), attacker.getUUID());
        return p == null || !p.members.contains(target.getUUID());
    }

    public static void createParty(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (getParty(server, player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("§cВы уже в пати."));
            return;
        }
        Party p = new Party(player.getUUID());
        PartyStateSaver.get(server).parties.add(p);
        PartyStateSaver.get(server).setDirty();
        player.sendSystemMessage(Component.literal("§aПати создана!"));
        syncParty(server, p);
    }

    public static void invitePlayer(ServerPlayer leader, ServerPlayer target) {
        MinecraftServer server = leader.level().getServer();
        Party p = getParty(server, leader.getUUID());
        int maxSize = ModConfig.getInstance().getGlobalInt("party.max_size", 2);

        if (p == null || !p.leader.equals(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§cВы не лидер пати."));
            return;
        }
        if (p.members.size() >= maxSize) {
            leader.sendSystemMessage(Component.literal("§cПати заполнена (макс. " + maxSize + ")."));
            return;
        }
        if (getParty(server, target.getUUID()) != null) {
            leader.sendSystemMessage(Component.literal("§cИгрок уже в пати."));
            return;
        }

        PENDING_INVITES.put(target.getUUID(), leader.getUUID());
        leader.sendSystemMessage(Component.literal("§aПриглашение отправлено игроку " + target.getName().getString()));

        Component msg = Component.literal("§eИгрок " + leader.getName().getString() + " приглашает вас в пати! ")
                .append(Component.literal("[Принять]").setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/party accept"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Нажмите, чтобы принять")))))
                .append(Component.literal(" "))
                .append(Component.literal("[Отклонить]").setStyle(Style.EMPTY
                        .withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/party decline"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Нажмите, чтобы отклонить")))));

        target.sendSystemMessage(msg);
    }

    public static void acceptInvite(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        UUID leaderUuid = PENDING_INVITES.remove(player.getUUID());
        if (leaderUuid == null) {
            player.sendSystemMessage(Component.literal("§cУ вас нет активных приглашений."));
            return;
        }
        Party p = getParty(server, leaderUuid);
        if (p == null) {
            player.sendSystemMessage(Component.literal("§cПати больше не существует."));
            return;
        }
        p.addMember(player.getUUID());
        PartyStateSaver.get(server).setDirty();

        player.sendSystemMessage(Component.literal("§aВы вступили в пати!"));
        for (UUID m : p.members) {
            ServerPlayer member = server.getPlayerList().getPlayer(m);
            if (member != null && member != player) {
                member.sendSystemMessage(Component.literal("§e" + player.getName().getString() + " вступил в пати!"));
            }
        }
        syncParty(server, p);
    }

    public static void leaveParty(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Party p = getParty(server, player.getUUID());
        if (p == null) {
            player.sendSystemMessage(Component.literal("§cВы не в пати."));
            return;
        }

        ModConfig cfg = ModConfig.getInstance();
        boolean disbandOnMember = cfg.getGlobalBool("party.disband_on_member_leave", false);
        boolean isLeader = p.leader.equals(player.getUUID());
        boolean disbandOnLeader = cfg.getGlobalBool("party.disband_on_leader_leave", false);

        if (disbandOnMember || (isLeader && disbandOnLeader)) {
            disbandParty(server, p, "Участник покинул пати");
            return;
        }

        p.removeMember(player.getUUID());
        if (p.members.isEmpty()) {
            PartyStateSaver.get(server).parties.remove(p);
        }
        PartyStateSaver.get(server).setDirty();

        player.sendSystemMessage(Component.literal("§cВы покинули пати."));
        for (UUID m : p.members) {
            ServerPlayer member = server.getPlayerList().getPlayer(m);
            if (member != null) member.sendSystemMessage(Component.literal("§e" + player.getName().getString() + " покинул пати."));
        }
        syncParty(server, p);
    }

    public static void handleDisconnect(ServerPlayer player) {
        PENDING_INVITES.remove(player.getUUID());

        MinecraftServer server = player.level().getServer();
        Party p = getParty(server, player.getUUID());
        if (p == null) return;

        ModConfig cfg = ModConfig.getInstance();
        boolean disbandOnMember = cfg.getGlobalBool("party.disband_on_member_leave", false);
        boolean isLeader = p.leader.equals(player.getUUID());
        boolean disbandOnLeader = cfg.getGlobalBool("party.disband_on_leader_leave", false);

        if (disbandOnMember || (isLeader && disbandOnLeader)) {
            disbandParty(server, p, "Игрок " + player.getName().getString() + " вышел из игры");
            return;
        }

        if (isLeader) {
            for (UUID m : p.members) {
                if (!m.equals(player.getUUID())) {
                    p.leader = m;
                    PartyStateSaver.get(server).setDirty();
                    ServerPlayer newLeader = server.getPlayerList().getPlayer(m);
                    if (newLeader != null) {
                        newLeader.sendSystemMessage(Component.literal("§eВы стали лидером пати, так как предыдущий лидер вышел."));
                    }
                    break;
                }
            }
        }
        syncParty(server, p);
    }

    public static void disbandParty(MinecraftServer server, Party p, String reason) {
        PartyStateSaver.get(server).parties.remove(p);
        PartyStateSaver.get(server).setDirty();
        for (UUID m : p.members) {
            ServerPlayer member = server.getPlayerList().getPlayer(m);
            if (member != null) {
                member.sendSystemMessage(Component.literal("§cПати распущена: " + reason));
                ServerPlayNetworking.send(member, new PartySyncPayload(List.of())); // Очищаем HUD
            }
        }
    }

    public static void syncParty(MinecraftServer server, Party p) {
        List<PartySyncPayload.Member> list = new ArrayList<>();
        for (UUID uuid : p.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) {
                String className = "Неизвестно";
                ClassDefinition def = ClassRegistry.get(PlayerClassData.get(sp).playerClass);
                if (def != null) className = def.displayName;

                list.add(new PartySyncPayload.Member(sp.getUUID(), sp.getName().getString(), sp.getHealth(), sp.getMaxHealth(), className));
            }
        }
        PartySyncPayload payload = new PartySyncPayload(list);
        for (UUID uuid : p.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) {
                ServerPlayNetworking.send(sp, payload);
            }
        }
    }
}