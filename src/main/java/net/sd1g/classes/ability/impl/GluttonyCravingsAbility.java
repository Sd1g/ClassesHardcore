package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class GluttonyCravingsAbility implements Ability {
    @Override public String getId() { return "cravings"; }
    @Override public String getDisplayName() { return "Чревоугодие"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/cravings"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("cravings_chance", 0.40) * 100);
        int dur = cfg.getIntEffect("cravings_hunger_duration_ticks", 140) / 20;
        return "С шансом " + chance + "% накладывает Голод III на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("cravings_chance", 0.40) * 100);
        return List.of("§7Шанс: §a" + chance + "%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("cravings_chance", 0.40);
        if (attacker.getRandom().nextDouble() < chance) {
            int amp = cfg.getIntEffect("cravings_hunger_amplifier", 2);
            int dur = cfg.getIntEffect("cravings_hunger_duration_ticks", 140);
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, amp, false, true));

            ServerLevel level = attacker.level();
            // Эффект на враге (зеленые искры и звук укуса)
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }
}