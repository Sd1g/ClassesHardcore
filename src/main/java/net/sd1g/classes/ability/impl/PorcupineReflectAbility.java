package net.sd1g.classes.ability.impl;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PorcupineReflectAbility implements Ability {
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> LAST_REFLECT_TICK = new HashMap<>();

    @Override public String getId() { return "porcupine_reflect"; }
    @Override public String getDisplayName() { return "Иглы"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("porcupine_reflect_chance", 0.30);
        double damage = cfg.getEffect("porcupine_reflect_damage", 3.0);
        double radius = cfg.getEffect("porcupine_reflect_radius", 3.0);
        return "При получении урона с шансом " + (int) Math.round(chance * 100) + "% наносит "
                + (int) damage + " урона всем в радиусе " + (int) radius + " блоков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("porcupine_reflect_chance", 0.30);
        double damage = cfg.getEffect("porcupine_reflect_damage", 3.0);
        double radius = cfg.getEffect("porcupine_reflect_radius", 3.0);
        int internalCd = cfg.getIntEffect("porcupine_reflect_internal_cooldown_ticks", 20);
        return List.of(
                "§7Шанс срабатывания: §a" + (int) Math.round(chance * 100) + "%",
                "§7Урон: §c" + (int) damage + " §7в радиусе §f" + (int) radius + " блоков",
                "§7Внутренний кулдаун: §f" + (internalCd / 20.0) + " сек."
        );
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        ModConfig cfg = ModConfig.getInstance();
        int internalCd = cfg.getIntEffect("porcupine_reflect_internal_cooldown_ticks", 20);

        long currentTick = player.level().getServer().getTickCount();
        Long last = LAST_REFLECT_TICK.get(player.getUUID());
        if (last != null && currentTick - last < internalCd) return;

        double chance = cfg.getEffect("porcupine_reflect_chance", 0.30);
        if (RNG.nextDouble() >= chance) return;

        LAST_REFLECT_TICK.put(player.getUUID(), currentTick);

        double radius = cfg.getEffect("porcupine_reflect_radius", 3.0);
        double damage = cfg.getEffect("porcupine_reflect_damage", 3.0);

        ServerLevel level = player.level();
        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> targets = level.getEntities(player, area, e -> e instanceof LivingEntity);

        DamageSource thorns = player.damageSources().thorns(player);
        for (Entity e : targets) {
            if (e == player) continue;
            if (e instanceof LivingEntity living) {
                living.hurtServer(level, thorns, (float) damage);
            }
        }

        PorcupineQuillsAbility.spawnReflectQuills(level, player, radius);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.2f, 1.4f);
    }

    public static void clearTracking(UUID uuid) {
        LAST_REFLECT_TICK.remove(uuid);
    }
}