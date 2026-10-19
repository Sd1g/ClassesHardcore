package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.InvisibilitySyncPayload;

import java.util.List;

public class InvisibilityAbility implements Ability {
    @Override public String getId() { return "invisibility"; }
    @Override public String getDisplayName() { return "Невидимость"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1200; }
    @Override public int getTickInterval() { return 1; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("invisibility_duration_ticks", 100) / 20;
        int mult = (int) Math.round(cfg.getEffect("ambush_damage_multiplier", 2.2) * 100) - 100;
        double passiveChance = cfg.getEffect("invisible_passive_invis_chance", 0.025) * 100;
        return "Невидимость на " + dur + " сек. Первый удар из невидимости наносит +" + mult
                + "% урона и раскрывает вас. Пассивно: " + String.format("%.1f", passiveChance) + "% шанс уйти в инвиз при получении урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("invisibility_duration_ticks", 100);
        int cd = cfg.getCooldown("invisibility", getBaseCooldown());
        return List.of(
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.invisibilityActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        applyInvisibility(player);
        return true;
    }

    private static void applyInvisibility(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        int duration = ModConfig.getInstance().getIntEffect("invisibility_duration_ticks", 100);

        data.invisibilityActiveTicks = duration;
        data.invisibleAmbushArmed = true;
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
        ServerPlayNetworking.send(player, new InvisibilitySyncPayload(duration));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        int points = 22;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            level.sendParticles(new DustParticleOptions(0xB34DFF, 1.5f), rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        for (int i = 0; i < 8; i++) {
            level.sendParticles(new DustParticleOptions(0x4D4DE6, 1.2f), x, y + 0.3 + i * 0.2, z, 1, 0.15, 0, 0.15, 0);
        }
        level.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 25, 0.6, 0.7, 0.6, 0.05);
        level.sendParticles(ParticleTypes.WITCH, x, y + 1.0, z, 20, 0.6, 0.7, 0.6, 0.03);
        level.sendParticles(new DustParticleOptions(0xB34DFF, 1.8f), x, y + 1.0, z, 25, 0.7, 0.8, 0.7, 0.1);
        level.sendParticles(new DustParticleOptions(0x4D4DE6, 1.5f), x, y + 1.5, z, 15, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.2, z, 10, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.3f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3f, 1.8f);

        player.sendOverlayMessage(Component.literal("§bНевидимость. §fДо конца " + (duration / 20) + " сек."));
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;
        double chance = ModConfig.getInstance().getEffect("invisible_passive_invis_chance", 0.025);
        if (player.getRandom().nextDouble() < chance) {
            applyInvisibility(player);
        }
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks <= 0) return;

        data.invisibilityActiveTicks--;

        if (data.invisibilityActiveTicks <= 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));

            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(ParticleTypes.POOF, x, y, z, 15, 0.6, 0.6, 0.6, 0.05);
            level.sendParticles(new DustParticleOptions(0xB34DFF, 1.2f), x, y, z, 12, 0.5, 0.5, 0.5, 0.08);
            level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.0f);

            player.sendOverlayMessage(Component.literal("§bНевидимость закончилась"));
            return;
        }

        ServerLevel level = player.level();
        if (data.invisibilityActiveTicks % 6 == 0) {
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.4, 0.4, 0.4, 0.02);
        }
        if (data.invisibilityActiveTicks % 15 == 0) {
            level.sendParticles(new DustParticleOptions(0xB34DFF, 0.8f), player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.4, 0.4, 0.4, 0.02);
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.invisibilityActiveTicks > 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            attacker.removeEffect(MobEffects.INVISIBILITY);
            PlayerClassData.markDirty(attacker);
            ServerPlayNetworking.send(attacker, new InvisibilitySyncPayload(0));

            ServerLevel level = attacker.level();
            double x = attacker.getX();
            double y = attacker.getY() + 1.0;
            double z = attacker.getZ();

            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.6, 0.6, 0.6, 0.05);
            level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.5f), x, y, z, 18, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(new DustParticleOptions(0xB34DFF, 1.0f), x, y, z, 12, 0.5, 0.5, 0.5, 0.1);

            level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.2f, 1.8f);

            attacker.sendOverlayMessage(Component.literal("§cВы раскрыты!"));
        }
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks > 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            player.removeEffect(MobEffects.INVISIBILITY);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));
        }
    }
}