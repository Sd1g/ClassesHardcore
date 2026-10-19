package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record InvisibilitySyncPayload(int activeTicks) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("invisibility_sync");
    public static final CustomPacketPayload.Type<InvisibilitySyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, InvisibilitySyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InvisibilitySyncPayload::activeTicks,
                    InvisibilitySyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}