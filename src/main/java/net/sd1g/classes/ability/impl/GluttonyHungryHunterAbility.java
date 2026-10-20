package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions; // ДОБАВЛЕН ИМПОРТ
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel; // ДОБАВЛЕН ИМПОРТ
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class GluttonyHungryHunterAbility implements Ability {
    @Override public String getId() { return "hungry_hunter"; }
    @Override public String getDisplayName() { return "Голодный хищник"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/hungry_hunter"); }

    @Override
    public String getDescription() {
        return "По цели с Голодом III или выше наносит дополнительно 1 НР чистого урона. Внутренний интервал 1.5 секунды.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус: §c+1 НР чистого урона");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        var hungerEffect = target.getEffect(MobEffects.HUNGER);
        if (hungerEffect == null || hungerEffect.getAmplifier() < 2) return;

        PlayerClassData data = PlayerClassData.get(attacker);
        long now = attacker.level().getGameTime();

        if (!data.isOnCooldown("hungry_hunter_cd", now)) {
            PureDamage.apply(attacker, target, 2.0f); // 1 HP = 2 урона
            data.setCooldown("hungry_hunter_cd", now, 30); // 1.5 сек = 30 тиков

            ServerLevel level = (ServerLevel) attacker.level();
            level.sendParticles(new DustParticleOptions(0xFF0000, 1.5f), target.getX(), target.getY() + 1, target.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
        }
    }
}