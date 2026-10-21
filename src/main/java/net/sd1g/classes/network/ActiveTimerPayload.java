package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record ActiveTimerPayload(String abilityId, int durationTicks) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("active_timer");
    public static final CustomPacketPayload.Type<ActiveTimerPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveTimerPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ActiveTimerPayload::abilityId,
                    ByteBufCodecs.VAR_INT, ActiveTimerPayload::durationTicks,
                    ActiveTimerPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}