package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record ClassSyncPayload(String classId) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("class_sync");
    public static final CustomPacketPayload.Type<ClassSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ClassSyncPayload::classId,
                    ClassSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}