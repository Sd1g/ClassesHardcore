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

public class IceDeepFreezeAbility implements Ability {
    @Override public String getId() { return "ice_deep_freeze"; }
    @Override public String getDisplayName() { return "Глубокая заморозка"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

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

        attacker.sendOverlayMessage(Component.literal("§bГлубокая заморозка!"));
    }
}