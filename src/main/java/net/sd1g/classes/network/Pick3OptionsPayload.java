package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

import java.util.List;

public record Pick3OptionsPayload(List<String> classIds) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("pick3_options");
    public static final Type<Pick3OptionsPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, Pick3OptionsPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Pick3OptionsPayload::classIds,
                    Pick3OptionsPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}