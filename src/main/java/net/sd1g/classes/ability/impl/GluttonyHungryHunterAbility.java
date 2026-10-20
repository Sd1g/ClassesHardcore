package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class GluttonyHungryHunterAbility implements Ability {
    @Override public String getId() { return "hungry_hunter"; }
    @Override public String getDisplayName() { return "Голодный хищник"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        double dmg = ModConfig.getInstance().getEffect("gluttony_hungry_hunter_damage", 2.0) / 2.0;
        return "По цели с Голодом III+ наносит +" + dmg + " НР чистого урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Внутренний интервал: 1.5 сек.");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        var hungerEffect = target.getEffect(MobEffects.HUNGER);
        if (hungerEffect == null || hungerEffect.getAmplifier() < 2) return;

        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();

        if (!data.isOnCooldown("hungry_hunter_cd", now)) {
            double dmg = ModConfig.getInstance().getEffect("gluttony_hungry_hunter_damage", 2.0);
            PureDamage.apply(attacker, target, (float) dmg);
            data.setCooldown("hungry_hunter_cd", now, 30);
        }
    }
}