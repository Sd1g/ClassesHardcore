package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class PorcupineQuillsAbility implements Ability {
    @Override public String getId() { return "porcupine_quills"; }
    @Override public String getDisplayName() { return "Шипы"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_quills"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("porcupine_quills", 400);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double dmg = cfg.getEffect("porcupine_active_damage", 6.0) / 2.0;
        int cd = cfg.getCooldown("porcupine_quills", 400) / 20;
        return "Наносит " + dmg + " НР чистого урона в радиусе. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Радиус растет от «Закалки шипов»");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);

        int perStep = cfg.getIntEffect("porcupine_hardening_hits_per_step", 500);
        double radPerStep = cfg.getEffect("porcupine_hardening_radius_per_step", 0.5);
        double maxSteps = cfg.getEffect("porcupine_hardening_max_steps", 4.0);

        // Радиус из конфига + бонус от закалки
        double radius = cfg.getEffect("porcupine_active_radius", 5.0) + Math.min((data.porcupineHitsTaken / (double)perStep) * radPerStep, maxSteps * radPerStep);
        float damage = (float) cfg.getEffect("porcupine_active_damage", 6.0);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // ЭФФЕКТ: Шар из шипов во все стороны
        // Мы используем параметры разброса (delta), чтобы заполнить сферу
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 150, radius, radius, radius, 0.2);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 50, radius, radius, radius, 0.1);

        // Звук вылетания шипов
        level.playSound(null, x, y, z, SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.5f);

        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> targets = level.getEntities(player, area, e -> e instanceof LivingEntity);

        for (Entity e : targets) {
            if (e != player && e instanceof LivingEntity living) {
                PureDamage.apply(player, living, damage);
            }
        }

        player.sendOverlayMessage(Component.literal("§eШипы. §fУрон " + (damage/2.0) + " НР в радиусе " + String.format("%.1f", radius) + " блоков."));
        return true;
    }
}