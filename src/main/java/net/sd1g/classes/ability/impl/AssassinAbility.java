package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HudMessagePayload;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class AssassinAbility implements Ability {
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("assassin_bonus_damage");

    @Override public String getId() { return "assassin"; }
    @Override public String getDisplayName() { return "Смертельный удар"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/assassin"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.005) * 100;
        return "С шансом " + String.format("%.1f", chance) + "% мгновенно убивает обычного моба, добавляя +" + cfg.getEffect("assassin_bonus_per_kill", 0.3) + " к базовому урону.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double bonus = AbilityClientState.assassinBonusDamage;
        return List.of(
                "§7Накопленный бонус: §c+" + String.format("%.1f", bonus) + " урона",
                "§7Прогресс сбрасывается при смене класса"
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (target instanceof Player) return;
        if (!(target instanceof Enemy)) return;

        ModConfig cfg = ModConfig.getInstance();

        double maxHpThreshold = cfg.getEffect("assassin_max_health_threshold", 100.0);
        if (target.getMaxHealth() > maxHpThreshold) return;

        double chance = cfg.getEffect("assassin_instakill_chance", 0.005);

        if (attacker.getRandom().nextDouble() < chance) {
            float pureDamage = (float) cfg.getEffect("assassin_pure_damage", 1000.0);
            PureDamage.apply(attacker, target, pureDamage);

            double bonus = cfg.getEffect("assassin_bonus_per_kill", 0.3);
            PlayerClassData data = PlayerClassData.get(attacker);
            data.assassinBonusDamage += bonus;
            PlayerClassData.markDirty(attacker);

            updateAttribute(attacker, data.assassinBonusDamage);

            ServerPlayNetworking.send(attacker, new net.sd1g.classes.network.AssassinSyncPayload(data.assassinBonusDamage));
            ServerPlayNetworking.send(attacker, new HudMessagePayload("§4СМЕРТЕЛЬНЫЙ УДАР! §c+" + bonus + " урона", 0xFFFF0000, 80));

            ServerLevel level = attacker.level();
            double x = target.getX();
            double y = target.getY() + (target.getBbHeight() / 2.0);
            double z = target.getZ();

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 5, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(new DustParticleOptions(0x000000, 3.0f), x, y, z, 80, 0.8, 0.8, 0.8, 0.2);
            level.sendParticles(new DustParticleOptions(0x8B0000, 2.5f), x, y, z, 60, 0.6, 0.6, 0.6, 0.15);
            level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 40, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(ParticleTypes.SOUL, x, y, z, 30, 0.4, 0.6, 0.4, 0.05);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 25, 0.5, 0.5, 0.5, 0.1);

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0xFF0000, 2.0f),
                        x + Math.cos(rad) * 2.5, target.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            }

            level.playSound(null, x, y, z, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.2f, 0.5f);
            level.playSound(null, x, y, z, SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, x, y, z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);

            attacker.sendOverlayMessage(Component.literal("§4Смертельный удар! §f+ " + bonus + " к урону."));
        }
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            AttributeModifier existing = attr.getModifier(MODIFIER_ID);
            double current = existing != null ? existing.amount() : 0.0;
            if (Math.abs(current - data.assassinBonusDamage) > 1e-6) {
                updateAttribute(player, data.assassinBonusDamage);
            }
        }
    }

    private void updateAttribute(ServerPlayer player, double bonus) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            if (bonus > 0) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
            } else {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.assassinBonusDamage = 0.0;
        updateAttribute(player, 0.0);
        ServerPlayNetworking.send(player, new net.sd1g.classes.network.AssassinSyncPayload(0.0));
    }
}