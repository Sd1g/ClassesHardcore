package net.sd1g.classes.ability;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface Ability {
    String getId();
    String getDisplayName();
    boolean isPassive();
    Identifier getIcon();
    TooltipData buildTooltip(Player player);

    default String getDescription(Player player) {
        TooltipData d = buildTooltip(player);
        return d.description() == null ? "" : d.description();
    }

    /** Время активации (каст). По умолчанию 0. */
    default int getCastTimeTicks(ServerPlayer player) { return 0; }

    default int getBaseCooldownTicks() { return 0; }
    default int getCooldownTicks(ServerPlayer player) { return getBaseCooldownTicks(); }

    default int getTickInterval() { return 1; }
    default boolean isHighlighted(Player player) { return false; }

    default boolean onActivate(ServerPlayer player) { return false; }
    default void onPassiveTick(ServerPlayer player) {}
    default void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {}
    default void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {}
    default void onRemove(ServerPlayer player) {}
}