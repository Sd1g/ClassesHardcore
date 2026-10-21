package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
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

public class IceDeepFreezeAbility implements Ability {
    @Override public String getId() { return "ice_deep_freeze"; }
    @Override public String getDisplayName() { return "Глубокая заморозка"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/ice_deep_freeze"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("ice_deep_freeze_chance", 0.03) * 100);
        int dur = cfg.getIntEffect("ice_deep_freeze_duration_ticks", 50) / 20;
        return "С вероятностью " + chance + "% полностью останавливает противника на " + dur + " секунды.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("ice_deep_freeze_chance", 0.03) * 100);
        return List.of("§7Шанс: §a" + chance + "%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("ice_deep_freeze_chance", 0.03);
        if (attacker.getRandom().nextDouble() >= chance) return;

        int freezeTicks = cfg.getIntEffect("ice_deep_freeze_duration_ticks", 50);
        int freezeAmp = cfg.getIntEffect("ice_deep_freeze_amplifier", 9);

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeTicks, freezeAmp, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, freezeTicks, freezeAmp, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() / 2.0);
        double z = target.getZ();

        // ЭПИЧНЫЙ И КРАСОЧНЫЙ ЭФФЕКТ ЛЕДЯНОЙ ТЮРЬМЫ
        // 1. Мощный взрыв снега и льда
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 60, 0.6, 0.8, 0.6, 0.1);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 40, 0.5, 0.8, 0.5, 0.2);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.5, 0.5, 0.5, 0.2);

        // 2. Яркие голубые и белые кольца пыли
        level.sendParticles(new DustParticleOptions(0x00FFFF, 2.5f), x, y, z, 50, 0.8, 0.8, 0.8, 0.2);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 2.0f), x, y, z, 30, 1.0, 1.0, 1.0, 0.1);

        // 3. Огромное кольцо по земле
        for (int i = 0; i < 360; i += 5) {
            double rad = Math.toRadians(i);
            level.sendParticles(new DustParticleOptions(0x87CEEB, 2.0f),
                    x + Math.cos(rad) * 2.0, target.getY() + 0.1, z + Math.sin(rad) * 2.0, 1, 0.0, 0.1, 0.0, 0.0);
        }

        // 4. Оглушительный звук замерзания и разбития
        level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);

        attacker.sendOverlayMessage(Component.literal("§bГлубокая заморозка!"));
    }
}