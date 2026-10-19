package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class PorcupineQuillsAbility implements Ability {
    @Override public String getId() { return "porcupine_quills"; }
    @Override public String getDisplayName() { return "Шипы"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 400; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double damage = cfg.getEffect("porcupine_active_damage", 5.0);
        double radius = cfg.getEffect("porcupine_active_radius", 5.0);
        return "Выстреливает иглами во все стороны. Наносит " + (int) damage
                + " урона всем в радиусе " + (int) radius + " блоков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double radius = cfg.getEffect("porcupine_active_radius", 5.0);
        double damage = cfg.getEffect("porcupine_active_damage", 5.0);
        int cd = cfg.getCooldown("porcupine_quills", getBaseCooldown());
        return List.of(
                "§7Урон: §c" + (int) damage + " §7всем в радиусе §f" + (int) radius + " блоков",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double radius = cfg.getEffect("porcupine_active_radius", 5.0);
        double damage = cfg.getEffect("porcupine_active_damage", 5.0);

        ServerLevel level = player.level();
        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> targets = level.getEntities(player, area, e -> e instanceof LivingEntity);

        DamageSource source = player.damageSources().playerAttack(player);
        for (Entity e : targets) {
            if (e == player) continue;
            if (e instanceof LivingEntity living) living.hurtServer(level, source, (float) damage);
        }

        spawnQuills(level, player, radius);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.3f, 1.4f);

        player.sendOverlayMessage(Component.literal(
                "§eШипы. §fУрон " + (int) damage + " в радиусе " + (int) radius + " блоков."));
        return true;
    }

    /** Кольцо + лучи из «игл» во все стороны. */
    static void spawnQuills(ServerLevel level, ServerPlayer player, double radius) {
        double cx = player.getX();
        double cy = player.getY() + 0.8;
        double cz = player.getZ();

        // Кольцо на двух высотах
        drawRing(level, cx, cy - 0.3, cz, radius, ParticleTypes.END_ROD);
        drawRing(level, cx, cy + 0.5, cz, radius, ParticleTypes.CRIT);

        // Жёлто-коричневое кольцо
        int points = (int) Math.max(28, radius * 12);
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(new DustParticleOptions(0xFFD91A, 1.3f), x, cy, z, 1, 0, 0, 0, 0);
            level.sendParticles(new DustParticleOptions(0x8B5A2B, 1.0f), x, cy + 0.3, z, 1, 0, 0, 0, 0);
        }

        // Лучи — 16 направлений, из иглы наружу
        int rays = 16;
        for (int i = 0; i < rays; i++) {
            double angle = i * Math.PI * 2 / rays;
            for (double r = 0.5; r <= radius; r += 0.4) {
                double x = cx + Math.cos(angle) * r;
                double z = cz + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, cy, z, 1, 0, 0, 0, 0);
                if (r < 2.0) {
                    level.sendParticles(new DustParticleOptions(0xFFD91A, 1.0f), x, cy, z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    static void spawnReflectQuills(ServerLevel level, ServerPlayer player, double radius) {
        double cx = player.getX();
        double cy = player.getY() + 0.8;
        double cz = player.getZ();
        drawRing(level, cx, cy - 0.3, cz, radius, ParticleTypes.CRIT);
        int points = (int) Math.max(16, radius * 8);
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(new DustParticleOptions(0xFF3333, 1.1f), x, cy, z, 1, 0, 0, 0, 0);
        }
    }

    private static void drawRing(ServerLevel level, double cx, double cy, double cz,
                                 double radius, ParticleOptions particle) {
        int points = (int) Math.max(24, radius * 12);
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(particle, x, cy, z, 1, 0, 0, 0, 0);
        }
    }
}