package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record UseAbilityPayload(String abilityId) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("use_ability");
    public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UseAbilityPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, UseAbilityPayload::abilityId,
                    UseAbilityPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}