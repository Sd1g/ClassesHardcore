package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

public class LuckyDiggerHeavyStrikeAbility implements Ability {
    @Override public String getId() { return "digger_heavy_strike"; }
    @Override public String getDisplayName() { return "Тяжёлый удар"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/digger_heavy_strike"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("digger_heavy_strike_chance", 0.05) * 100);
        int dur = cfg.getIntEffect("digger_heavy_strike_duration", 300) / 20;
        return "С вероятностью " + chance + "% накладывает Утомление V на " + dur + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("digger_heavy_strike_chance", 0.05) * 100);
        return List.of("§7Шанс: §f" + chance + "%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damage) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("digger_heavy_strike_chance", 0.05);

        if (attacker.getRandom().nextDouble() < chance) {
            int dur = cfg.getIntEffect("digger_heavy_strike_duration", 300);
            int amp = cfg.getIntEffect("digger_heavy_strike_amp", 4);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, dur, amp, true, true));

            ServerLevel level = attacker.level();
            double x = target.getX();
            double y = target.getY() + (target.getBbHeight() / 2.0);
            double z = target.getZ();

            // Крутые эффекты на враге
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 15, 0.3, 0.3, 0.3, 0.2);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.2, 0.2, 0.2, 0.1);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 0.5, z, 3, 0.2, 0.2, 0.2, 0.0);

            // Звук сочного удара
            level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.2f);
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

            attacker.sendOverlayMessage(Component.literal("§6Тяжёлый удар! §fУтомление V."));
        }
    }
}