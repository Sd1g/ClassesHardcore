package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record ArmedStatePayload(String abilityId, boolean armed) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("armed_state");
    public static final CustomPacketPayload.Type<ArmedStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmedStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ArmedStatePayload::abilityId,
                    ByteBufCodecs.BOOL, ArmedStatePayload::armed,
                    ArmedStatePayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}