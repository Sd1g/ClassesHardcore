package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class GluttonyCravingsAbility implements Ability {
    @Override public String getId() { return "cravings"; }
    @Override public String getDisplayName() { return "Чревоугодие"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/cravings"); }

    @Override
    public String getDescription() {
        return "Каждый удар с вероятностью 40% накладывает Голод III на 7 секунд.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §a40%", "§7Эффект: §6Голод III §7на §f7 сек.");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (attacker.getRandom().nextDouble() < 0.40) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 2, false, true)); // 7 сек, Голод III
            ServerLevel level = attacker.level();
            level.sendParticles(new DustParticleOptions(0xAA4400, 1.4f), target.getX(), target.getY() + 1, target.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
        }
    }
}