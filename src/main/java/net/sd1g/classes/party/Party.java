package net.sd1g.classes.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    public final UUID id;
    public UUID leader;
    public final List<UUID> members = new ArrayList<>();

    public Party(UUID leader) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.members.add(leader);
    }

    public void addMember(UUID player) {
        if (!members.contains(player)) members.add(player);
    }

    public void removeMember(UUID player) {
        members.remove(player);
        if (leader.equals(player) && !members.isEmpty()) {
            leader = members.getFirst();
        }
    }
}