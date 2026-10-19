package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record ClassTokensSyncPayload(int random, int pick3) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("class_tokens_sync");
    public static final Type<ClassTokensSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassTokensSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClassTokensSyncPayload::random,
                    ByteBufCodecs.VAR_INT, ClassTokensSyncPayload::pick3,
                    ClassTokensSyncPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}