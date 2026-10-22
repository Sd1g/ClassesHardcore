package net.sd1g.classes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;

public record CooldownSyncPayload(String abilityId, int remainingTicks, int totalTicks) implements CustomPacketPayload {
    public static final Identifier ID = ClassesHardcore.id("cooldown_sync");
    public static final CustomPacketPayload.Type<CooldownSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    // Удобный 2-арг конструктор: total = remaining (как было раньше)
    public CooldownSyncPayload(String abilityId, int remainingTicks) {
        this(abilityId, remainingTicks, remainingTicks);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CooldownSyncPayload::abilityId,
                    ByteBufCodecs.VAR_INT, CooldownSyncPayload::remainingTicks,
                    ByteBufCodecs.VAR_INT, CooldownSyncPayload::totalTicks,
                    CooldownSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}