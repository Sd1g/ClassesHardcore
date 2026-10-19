package net.sd1g.classes.ability;

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
    int getBaseCooldown();

    default String getDescription() { return ""; }
    default List<String> getExtraTooltipLines(Player player) { return List.of(); }
    default boolean isHighlighted(Player player) { return false; }

    default Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("classeshardcore",
                "textures/gui/sprites/ability/" + getId() + ".png");
    }

    default int getTickInterval() { return 20; }

    /** Динамический кулдаун (например, для телепорта в зависимости от дистанции). */
    default int getDynamicCooldown(ServerPlayer player) { return getBaseCooldown(); }

    default boolean onActivate(ServerPlayer player) { return true; }
    default void onPassiveTick(ServerPlayer player) {}
    default void onHit(ServerPlayer attacker, LivingEntity target) {}
    default void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {}
    default void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {}
}