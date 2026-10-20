package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VampireBloodKissAbility implements Ability {
    private static final Map<UUID, List<StolenHeart>> STOLEN_HEARTS = new HashMap<>();
    private record StolenHeart(Identifier id, long expireTick, UUID targetUuid) {}

    @Override public String getId() { return "vampire_blood_kiss"; }
    @Override public String getDisplayName() { return "Кровавый поцелуй"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("vampire_steal_heart_chance", 0.004) * 100;
        return "Шанс " + String.format("%.1f", chance) + "% украсть 1 сердце на 1 час.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает только по игрокам");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        List<StolenHeart> hearts = STOLEN_HEARTS.get(player.getUUID());
        if (hearts != null) {
            AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                hearts.removeIf(h -> {
                    if (now >= h.expireTick()) {
                        attr.removeModifier(h.id());
                        ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(h.targetUuid());
                        if (target != null) {
                            AttributeInstance targetAttr = target.getAttribute(Attributes.MAX_HEALTH);
                            if (targetAttr != null) targetAttr.removeModifier(h.id());
                        }
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (target instanceof ServerPlayer targetPlayer) {
            ModConfig cfg = ModConfig.getInstance();
            double chance = cfg.getEffect("vampire_steal_heart_chance", 0.004);

            if (attacker.getRandom().nextDouble() < chance) {
                double amount = cfg.getEffect("vampire_steal_heart_amount", 2.0);
                long expire = attacker.level().getGameTime() + (long) cfg.getEffect("vampire_steal_heart_duration_ticks", 72000.0);

                Identifier modId = Identifier.fromNamespaceAndPath("classeshardcore", "steal_" + UUID.randomUUID());

                AttributeInstance attackerAttr = attacker.getAttribute(Attributes.MAX_HEALTH);
                if (attackerAttr != null) attackerAttr.addOrUpdateTransientModifier(new AttributeModifier(modId, amount, AttributeModifier.Operation.ADD_VALUE));

                AttributeInstance targetAttr = targetPlayer.getAttribute(Attributes.MAX_HEALTH);
                if (targetAttr != null) targetAttr.addOrUpdateTransientModifier(new AttributeModifier(modId, -amount, AttributeModifier.Operation.ADD_VALUE));

                STOLEN_HEARTS.computeIfAbsent(attacker.getUUID(), k -> new ArrayList<>()).add(new StolenHeart(modId, expire, targetPlayer.getUUID()));
                attacker.sendSystemMessage(Component.literal("§cВы украли сердце!"));
                targetPlayer.sendSystemMessage(Component.literal("§cВампир украл у вас сердце!"));
            }
        }
    }
}