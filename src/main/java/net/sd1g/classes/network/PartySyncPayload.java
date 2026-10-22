package net.sd1g.classes.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

import java.util.List;
import java.util.UUID;

public record PartySyncPayload(List<Member> members) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("party_sync");
    public static final Type<PartySyncPayload> TYPE = new Type<>(ID);

    public record Member(UUID uuid, String name, float health, float maxHealth, String classDisplayName) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Member> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, Member::uuid,
                        ByteBufCodecs.STRING_UTF8, Member::name,
                        ByteBufCodecs.FLOAT, Member::health,
                        ByteBufCodecs.FLOAT, Member::maxHealth,
                        ByteBufCodecs.STRING_UTF8, Member::classDisplayName,
                        Member::new
                );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PartySyncPayload> CODEC =
            StreamCodec.composite(
                    Member.CODEC.apply(ByteBufCodecs.list()), PartySyncPayload::members,
                    PartySyncPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}