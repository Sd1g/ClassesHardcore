package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HudMessagePayload;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class PorcupineReflectAbility implements Ability {
    @Override public String getId() { return "porcupine_reflect"; }
    @Override public String getDisplayName() { return "Иглы"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_reflect"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("porcupine_reflect_chance", 0.30) * 100);
        double dmg = cfg.getEffect("porcupine_reflect_damage", 2.0);
        return "С шансом " + chance + "% наносит " + dmg + " НР чистого урона атакующему.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Внутренний интервал 2 сек.");
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (data.isOnCooldown("porcupine_reflect_cd", now)) return;
        if (player.getRandom().nextDouble() >= cfg.getEffect("porcupine_reflect_chance", 0.30)) return;

        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            double radius = cfg.getEffect("porcupine_reflect_radius", 3.0);
            if (attacker.distanceToSqr(player) <= radius * radius) {
                float dmg = (float) cfg.getEffect("porcupine_reflect_damage", 2.0);
                PureDamage.apply(player, attacker, dmg);

                int cdTicks = cfg.getIntEffect("porcupine_reflect_internal_cooldown_ticks", 40);
                data.setCooldown("porcupine_reflect_cd", now, cdTicks);

                ServerLevel level = player.level();
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();

                level.sendParticles(ParticleTypes.CRIT, x, y, z, 60, radius, radius, radius, 0.1);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 2, radius, radius, radius, 0.1);

                level.playSound(null, x, y, z, SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.0f, 1.4f);

                ServerPlayNetworking.send(player, new HudMessagePayload("§cИглы!", 0xFFFF5555, 40));
            }
        }
    }
}