package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record RunnerSyncPayload(int activeTicks) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("runner_sync");
    public static final CustomPacketPayload.Type<RunnerSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RunnerSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RunnerSyncPayload::activeTicks,
                    RunnerSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}