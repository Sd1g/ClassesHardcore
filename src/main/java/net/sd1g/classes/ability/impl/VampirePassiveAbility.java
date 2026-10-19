package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class VampirePassiveAbility implements Ability {
    private static final Random RNG = new Random();
    private static final Map<UUID, List<StolenHeart>> STOLEN_HEARTS = new HashMap<>();

    private record StolenHeart(Identifier id, long expireTick, UUID targetUuid) {}

    @Override public String getId() { return "vampire_lifesteal"; }
    @Override public String getDisplayName() { return "Жажда крови"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("vampire_passive_chance", 0.20) * 100);
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.60) * 100);
        double stealChance = cfg.getEffect("vampire_steal_heart_chance", 0.004) * 100;
        return "С шансом " + chance + "% лечит на " + percent
                + "% от урона. Шанс " + String.format("%.1f", stealChance) + "% украсть сердце у игрока (1 час).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.60) * 100);
        int chance = (int) Math.round(cfg.getEffect("vampire_passive_chance", 0.20) * 100);
        double stealChance = cfg.getEffect("vampire_steal_heart_chance", 0.004) * 100;
        return List.of(
                "§7Шанс срабатывания: §a" + chance + "%",
                "§7Лечение: §a" + percent + "% §7от урона",
                "§7Кража сердца: §c" + String.format("%.1f", stealChance) + "%"
        );
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
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks > 0) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("vampire_passive_chance", 0.20);
        if (RNG.nextDouble() < chance) {
            double percent = cfg.getEffect("vampire_heal_percent", 0.60);
            attacker.heal((float) (damageDealt * percent));

            ServerLevel level = attacker.level();
            double tx = target.getX();
            double ty = target.getY() + target.getBbHeight() * 0.5;
            double tz = target.getZ();

            level.sendParticles(ParticleTypes.CRIMSON_SPORE, tx, ty, tz, 12, 0.5, 0.5, 0.5, 0.03);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tx, ty, tz, 6, 0.4, 0.4, 0.4, 0.03);
            level.sendParticles(new DustParticleOptions(0xFF1A1A, 1.4f), tx, ty, tz, 10, 0.4, 0.5, 0.4, 0.1);
            level.sendParticles(new DustParticleOptions(0xCC0000, 1.2f), tx, ty, tz, 6, 0.4, 0.5, 0.4, 0.08);

            level.playSound(null, tx, ty, tz, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.7f, 1.4f);
        }

        // Кража сердца
        if (target instanceof ServerPlayer targetPlayer) {
            double stealChance = cfg.getEffect("vampire_steal_heart_chance", 0.004);
            if (RNG.nextDouble() < stealChance) {
                double amount = cfg.getEffect("vampire_steal_heart_amount", 2.0);
                long expire = attacker.level().getGameTime() + (long) cfg.getEffect("vampire_steal_heart_duration_ticks", 72000.0);

                Identifier modId = Identifier.fromNamespaceAndPath("classeshardcore", "steal_" + UUID.randomUUID());

                AttributeInstance attackerAttr = attacker.getAttribute(Attributes.MAX_HEALTH);
                if (attackerAttr != null) {
                    attackerAttr.addOrUpdateTransientModifier(new AttributeModifier(modId, amount, AttributeModifier.Operation.ADD_VALUE));
                }
                AttributeInstance targetAttr = targetPlayer.getAttribute(Attributes.MAX_HEALTH);
                if (targetAttr != null) {
                    targetAttr.addOrUpdateTransientModifier(new AttributeModifier(modId, -amount, AttributeModifier.Operation.ADD_VALUE));
                }

                STOLEN_HEARTS.computeIfAbsent(attacker.getUUID(), k -> new ArrayList<>()).add(new StolenHeart(modId, expire, targetPlayer.getUUID()));

                attacker.sendSystemMessage(Component.literal("§cВы украли сердце у " + targetPlayer.getName().getString() + "!"));
                targetPlayer.sendSystemMessage(Component.literal("§cВампир украл у вас сердце!"));
            }
        }
    }
}