package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record AssassinSyncPayload(double bonusDamage) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("assassin_sync");
    public static final CustomPacketPayload.Type<AssassinSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AssassinSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, AssassinSyncPayload::bonusDamage,
                    AssassinSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}