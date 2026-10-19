package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class ArcherJumpBackAbility implements Ability {
    @Override public String getId() { return "archer_jump_back"; }
    @Override public String getDisplayName() { return "Разрыв дистанции"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 700; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dist = (int) cfg.getEffect("archer_jump_back_distance", 12.0);
        int hp = (int) cfg.getEffect("archer_jump_back_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("archer_jump_back_absorption_duration_ticks", 1200) / 20;
        int cdSec = cfg.getCooldown("archer_jump_back", getBaseCooldown()) / 20;
        return "Рывок назад на " + dist + " блоков + " + (hp / 2) + " золотых сердец на "
                + dur + " сек. Кулдаун " + cdSec + " сек. (Складывается до 6 сердец)";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dist = (int) cfg.getEffect("archer_jump_back_distance", 12.0);
        int hp = (int) cfg.getEffect("archer_jump_back_absorption_hp", 4.0);
        int dur = cfg.getIntEffect("archer_jump_back_absorption_duration_ticks", 1200);
        int cd = cfg.getCooldown("archer_jump_back", getBaseCooldown());
        return List.of(
                "§7Дистанция: §f" + dist + " блоков",
                "§7Поглощение: §6+" + hp + " HP §7на §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        double dist = cfg.getEffect("archer_jump_back_distance", 12.0);
        int hp = (int) cfg.getEffect("archer_jump_back_absorption_hp", 4.0);
        int durTicks = cfg.getIntEffect("archer_jump_back_absorption_duration_ticks", 1200);
        double maxHearts = cfg.getEffect("archer_absorption_max_hearts", 6.0);

        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        if (back.lengthSqr() < 1e-6) {
            double yaw = Math.toRadians(player.getYRot());
            back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
        }
        back = back.normalize();

        double strength = Math.min(3.0, dist / 5.0);
        double vy = 0.45;

        player.setDeltaMovement(back.x * strength, vy, back.z * strength);
        player.hurtMarked = true;

        // Сложение золотых сердец
        float currentAbs = player.getAbsorptionAmount();
        float maxAbs = (float) (maxHearts * 2); // 6 сердец = 12 HP
        float newAbs = Math.min(maxAbs, currentAbs + hp);
        int amp = Math.max(0, (int) (newAbs / 4) - 1);

        player.removeEffect(MobEffects.ABSORPTION);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durTicks, amp, false, true, true));
        player.setAbsorptionAmount(newAbs); // Принудительно выставляем точное количество HP

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.3, z, 20, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.5, z, 5, 0.4, 0.3, 0.4, 0.0);
        level.sendParticles(new DustParticleOptions(0xDDA030, 1.6f), x, y + 0.5, z, 20, 0.6, 0.4, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.2f), x, y + 1.0, z, 12, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.8, z, 10, 0.4, 0.4, 0.4, 0.05);

        for (int i = 0; i < 8; i++) {
            level.sendParticles(new DustParticleOptions(0xFFD700, 1.0f), x, y + 0.6 + i * 0.1, z, 1, 0.2, 0, 0.2, 0);
        }

        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.3f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.2f, 1.5f);

        player.sendOverlayMessage(Component.literal("§eРазрыв дистанции."));
        return true;
    }
}