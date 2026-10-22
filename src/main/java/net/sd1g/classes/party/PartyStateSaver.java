package net.sd1g.classes.party;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sd1g.classes.ClassesHardcore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyStateSaver extends SavedData {
    public final List<Party> parties = new ArrayList<>();

    public static final Codec<PartyStateSaver> CODEC = CompoundTag.CODEC.xmap(
            PartyStateSaver::load,
            PartyStateSaver::saveToTag
    );

    public static final SavedDataType<PartyStateSaver> TYPE = new SavedDataType<>(
            ClassesHardcore.id("party_data"),
            PartyStateSaver::new,
            CODEC,
            null
    );

    public static PartyStateSaver get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PartyStateSaver load(CompoundTag tag) {
        PartyStateSaver saver = new PartyStateSaver();
        ListTag list = tag.getListOrEmpty("parties");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag pTag = list.getCompoundOrEmpty(i);
            try {
                UUID leader = UUID.fromString(pTag.getStringOr("leader", ""));
                Party party = new Party(leader);
                ListTag memList = pTag.getListOrEmpty("members");
                for (int j = 0; j < memList.size(); j++) {
                    party.addMember(UUID.fromString(memList.getString(j).orElse("")));
                }
                saver.parties.add(party);
            } catch (Exception ignored) {}
        }
        return saver;
    }

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Party p : parties) {
            CompoundTag pTag = new CompoundTag();
            pTag.putString("leader", p.leader.toString());
            ListTag memList = new ListTag();
            for (UUID m : p.members) memList.add(net.minecraft.nbt.StringTag.valueOf(m.toString()));
            pTag.put("members", memList);
            list.add(pTag);
        }
        tag.put("parties", list);
        return tag;
    }
}