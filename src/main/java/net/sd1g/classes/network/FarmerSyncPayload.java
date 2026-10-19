package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record FarmerSyncPayload(int mobKills, int playerKills) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("farmer_sync");
    public static final CustomPacketPayload.Type<FarmerSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, FarmerSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FarmerSyncPayload::mobKills,
                    ByteBufCodecs.VAR_INT, FarmerSyncPayload::playerKills,
                    FarmerSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}