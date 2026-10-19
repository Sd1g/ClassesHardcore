package net.sd1g.classes.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sd1g.classes.ClassesHardcore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClassStateSaver extends SavedData {
    public final Map<UUID, PlayerClassData> playerData = new HashMap<>();

    // Codec для сериализации/десериализации всего объекта ClassStateSaver
    public static final Codec<ClassStateSaver> CODEC = CompoundTag.CODEC.xmap(
            ClassStateSaver::load,     // из CompoundTag -> ClassStateSaver
            ClassStateSaver::saveToTag // из ClassStateSaver -> CompoundTag
    );

    public static final SavedDataType<ClassStateSaver> TYPE = new SavedDataType<>(
            ClassesHardcore.id("player_data"), // уникальный идентификатор
            ClassStateSaver::new,               // конструктор по умолчанию
            CODEC,                              // Codec для сериализации
            null                                // DataFixer не нужен
    );

    public static ClassStateSaver get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static ClassStateSaver load(CompoundTag tag) {
        ClassStateSaver saver = new ClassStateSaver();
        CompoundTag players = tag.getCompoundOrEmpty("players");
        for (String uuidStr : players.keySet()) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                saver.playerData.put(uuid, PlayerClassData.fromNbt(players.getCompoundOrEmpty(uuidStr)));
            } catch (IllegalArgumentException ignored) {}
        }
        return saver;
    }

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag players = new CompoundTag();
        playerData.forEach((uuid, data) -> players.put(uuid.toString(), data.toNbt()));
        tag.put("players", players);
        return tag;
    }
}