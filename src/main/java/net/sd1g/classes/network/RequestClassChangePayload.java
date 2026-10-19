package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

// actionType: 0 = случайный, 1 = выбор из 3 (открыть меню), 2 = админский (применить напрямую), 3 = подтверждение выбора из 3
public record RequestClassChangePayload(int actionType, String classId) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("request_class_change");
    public static final Type<RequestClassChangePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClassChangePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RequestClassChangePayload::actionType,
                    ByteBufCodecs.STRING_UTF8, RequestClassChangePayload::classId,
                    RequestClassChangePayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}