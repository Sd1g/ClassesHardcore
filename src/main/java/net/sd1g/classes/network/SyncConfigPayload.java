package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record SyncConfigPayload(String json) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("sync_config");
    public static final Type<SyncConfigPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncConfigPayload::json,
                    SyncConfigPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}