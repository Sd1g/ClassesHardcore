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
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ActiveTimerPayload;
import net.sd1g.classes.network.VampireSyncPayload;

import java.util.List;

public class VampireDrainAbility implements Ability {
    @Override public String getId() { return "vampire_drain"; }
    @Override public String getDisplayName() { return "Вампиризм"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/vampire_drain"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("vampire_drain", 1200);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("vampire_active_duration_ticks", 200) / 20;
        int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.70) * 100);
        int cd = cfg.getCooldown("vampire_drain", 1200) / 20;
        return "На " + dur + " сек удары лечат на " + percent + "% урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int percent = (int) Math.round(ModConfig.getInstance().getEffect("vampire_heal_percent", 0.70) * 100);
        return List.of("§7Лечение: §a" + percent + "% §7от урона");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.vampireActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        int duration = ModConfig.getInstance().getIntEffect("vampire_active_duration_ticks", 200);
        PlayerClassData data = PlayerClassData.get(player);
        data.vampireActiveTicks = duration;
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new VampireSyncPayload(duration));
        ServerPlayNetworking.send(player, new ActiveTimerPayload("vampire_drain", duration));

        ServerLevel level = player.level();

        // Зрелищная активация: Крик и сердцебиение
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 0.5f); // Низкий крик
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5f, 1.2f); // Сердцебиение

        // Взрыв темно-красной крови при активации
        level.sendParticles(new DustParticleOptions(0x8B0000, 2.0f), player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

        player.sendOverlayMessage(Component.literal("§cВампиризм активирован!"));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.vampireActiveTicks <= 0) return;

        // Кровавая аура вокруг вампира, пока способность активна
        ServerLevel level = player.level();
        if (player.tickCount % 3 == 0) {
            double angle = (player.tickCount * 0.2) % (Math.PI * 2);
            double radius = 0.8;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            level.sendParticles(new DustParticleOptions(0xFF0000, 1.2f), x, player.getY() + 0.5 + (Math.sin(player.tickCount * 0.1) * 0.3), z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.5f), x, player.getY() + 1.0, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        if (player.tickCount % 40 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5f, 1.0f);
        }

        data.vampireActiveTicks--;
        if (data.vampireActiveTicks <= 0) {
            data.vampireActiveTicks = 0;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new VampireSyncPayload(0));
            player.sendOverlayMessage(Component.literal("§cВампиризм закончился"));
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks <= 0) return;

        double percent = ModConfig.getInstance().getEffect("vampire_heal_percent", 0.70);
        attacker.heal((float) (damageDealt * percent));

        ServerLevel level = attacker.level();
        // Урон на цели, хил на игроке
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
    }
}