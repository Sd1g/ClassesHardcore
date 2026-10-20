package net.sd1g.classes.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface Ability {
    String getId();
    String getDisplayName();
    boolean isPassive();
    String getDescription();

    default int getBaseCooldown() { return 0; }
    default Identifier getIcon() { return null; }
    default int getTickInterval() { return 1; }
    default boolean isHighlighted(Player player) { return false; }
    default List<String> getExtraTooltipLines(Player player) { return List.of(); }

    default boolean onActivate(ServerPlayer player) { return false; }
    default void onPassiveTick(ServerPlayer player) {}
    default void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {}
    default void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {}
    default int getDynamicCooldown(ServerPlayer player) { return getBaseCooldown(); }

    // НОВОЕ: Метод для очистки внутренних состояний при смене класса
    default void onRemove(ServerPlayer player) {}
}