package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record HudMessagePayload(String text, int color, int durationTicks) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("hud_message");
    public static final CustomPacketPayload.Type<HudMessagePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HudMessagePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, HudMessagePayload::text,
                    ByteBufCodecs.VAR_INT, HudMessagePayload::color,
                    ByteBufCodecs.VAR_INT, HudMessagePayload::durationTicks,
                    HudMessagePayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}