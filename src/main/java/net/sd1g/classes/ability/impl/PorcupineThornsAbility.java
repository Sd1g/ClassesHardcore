package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PorcupineThornsAbility implements Ability {
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> LAST_THORNS_TICK = new HashMap<>();
    private static final int INTERNAL_COOLDOWN_TICKS = 20;

    @Override public String getId() { return "porcupine_thorns"; }
    @Override public String getDisplayName() { return "Игольчатый барьер"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("porcupine_thorns_chance", 0.05) * 100);
        int percent = (int) Math.round(cfg.getEffect("porcupine_thorns_percent", 0.30) * 100);
        return "С шансом " + chance + "% возвращает атакующему " + percent + "% от полученного урона.";
    }

    @Override
    public java.util.List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("porcupine_thorns_chance", 0.05) * 100);
        int percent = (int) Math.round(cfg.getEffect("porcupine_thorns_percent", 0.30) * 100);
        return java.util.List.of(
                "§7Шанс: §a" + chance + "%",
                "§7Возврат урона: §c" + percent + "%"
        );
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("porcupine_thorns_chance", 0.05);
        double percent = cfg.getEffect("porcupine_thorns_percent", 0.30);

        long currentTick = player.level().getServer().getTickCount();
        Long last = LAST_THORNS_TICK.get(player.getUUID());
        if (last != null && currentTick - last < INTERNAL_COOLDOWN_TICKS) return;

        if (RNG.nextDouble() >= chance) return;

        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (attacker == player) return;
        if (attacker.distanceToSqr(player) > 16.0) return;

        LAST_THORNS_TICK.put(player.getUUID(), currentTick);

        float dmg = (float) (damageTaken * percent);
        if (dmg < 1.0f) dmg = 1.0f;

        ServerLevel level = player.level();
        DamageSource thorns = player.damageSources().thorns(player);
        attacker.hurtServer(level, thorns, dmg);

        // Вспышка на атакующем
        double ax = attacker.getX();
        double ay = attacker.getY() + attacker.getBbHeight() * 0.5;
        double az = attacker.getZ();
        level.sendParticles(ParticleTypes.CRIT, ax, ay, az, 10, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(new DustParticleOptions(0xFFD91A, 1.2f), ax, ay, az, 8, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(new DustParticleOptions(0x8B5A2B, 1.0f), ax, ay, az, 6, 0.3, 0.3, 0.3, 0.05);

        level.playSound(null, ax, ay, az, SoundEvents.PUFFER_FISH_STING,
                SoundSource.PLAYERS, 1.0f, 1.7f);
    }

    public static void clearTracking(UUID uuid) {
        LAST_THORNS_TICK.remove(uuid);
    }
}