package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record LuckyDiggerSyncPayload(String mode, int totalOre, int level) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("lucky_digger_sync");
    public static final CustomPacketPayload.Type<LuckyDiggerSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, LuckyDiggerSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, LuckyDiggerSyncPayload::mode,
                    ByteBufCodecs.VAR_INT, LuckyDiggerSyncPayload::totalOre,
                    ByteBufCodecs.VAR_INT, LuckyDiggerSyncPayload::level,
                    LuckyDiggerSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}