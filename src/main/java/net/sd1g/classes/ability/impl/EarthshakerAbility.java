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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ArmedStatePayload;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class EarthshakerAbility implements Ability {
    @Override public String getId() { return "earthshaker"; }
    @Override public String getDisplayName() { return "Дрожь земли"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/earthshaker"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("earthshaker", 500);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        double dmg = cfg.getEffect("earthshaker_pure_damage", 2.0) / 2.0;
        int cd = getBaseCooldown() / 20;
        return "Следующий удар подбрасывает врага на " + min + "-" + max + " блоков и наносит " + dmg + " НР чистого урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает только по мобам/игрокам");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.bruteShakeArmed = true;
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new ArmedStatePayload("earthshaker", true));

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        level.sendParticles(new DustParticleOptions(0x8B0000, 1.5f), x, y, z, 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.5f, 0.8f);

        player.sendOverlayMessage(Component.literal("§eДрожь земли готова!"));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.bruteShakeArmed) return;

        data.bruteShakeArmed = false;
        PlayerClassData.markDirty(attacker);
        ServerPlayNetworking.send(attacker, new ArmedStatePayload("earthshaker", false));

        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        double dmg = cfg.getEffect("earthshaker_pure_damage", 2.0);

        int launch = min + attacker.getRandom().nextInt(Math.max(1, max - min + 1));

        // ИСПРАВЛЕНИЕ: Убрали hasImpulse, оставили только hurtMarked
        double upwardVelocity = launch * 0.15;
        target.setDeltaMovement(target.getDeltaMovement().x, upwardVelocity, target.getDeltaMovement().z);
        target.hurtMarked = true;
        target.fallDistance = 0.0F; // Сбрасываем урон от падения, чтобы моб не убился об землю

        PureDamage.apply(attacker, target, (float)dmg);

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        // ==========================================
        // ЭФФЕКТ ВЗРЫВА И РЫВКА ВВЕРХ
        // ==========================================

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(new DustParticleOptions(0xFF4500, 2.0f), x, y, z, 40, 0.5, 0.2, 0.5, 0.1);

        for (int i = 0; i < launch * 2; i++) {
            level.sendParticles(ParticleTypes.CLOUD, x, y + (i * 0.2), z, 2, 0.2, 0.0, 0.2, 0.05);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + (i * 0.2), z, 1, 0.1, 0.0, 0.1, 0.0);
        }

        level.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.2, 0.5, 0.2, 0.05);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.2f, 0.6f);
    }
}