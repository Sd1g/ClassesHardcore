package net.sd1g.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ability.impl.TeleporterAbility;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(LivingEntity.class)
public class TeleporterLastChanceMixin {

    private static final Random RNG = new Random();

    @Inject(
            method = "checkTotemDeathProtection",
            at = @At("RETURN"),
            require = 0
    )
    private void classeshardcore$onTotemUsed(CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!((Object) this instanceof ServerPlayer sp)) return;

        PlayerClassData data = PlayerClassData.get(sp);
        if (!"teleporter".equals(data.playerClass)) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("last_chance_chance", 0.20);
        if (RNG.nextDouble() >= chance) return;

        double radius = cfg.getEffect("last_chance_radius", 10.0);
        doRandomTeleport(sp, radius);
    }

    private static void doRandomTeleport(ServerPlayer player, double radius) {
        ServerLevel level = player.level();
        Vec3 origin = player.position();

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = 3 + RNG.nextDouble() * Math.max(0, radius - 3);
            double tx = origin.x + Math.cos(angle) * dist;
            double tz = origin.z + Math.sin(angle) * dist;
            Vec3 rawTarget = new Vec3(tx, origin.y, tz);

            BlockPos standBp = TeleporterAbility.findTargetPosition(level, rawTarget);
            if (standBp == null) continue;

            double px = player.getX();
            double py = player.getY() + 1.0;
            double pz = player.getZ();

            // ЭПИЧНЫЙ ЭФФЕКТ СПАСЕНИЯ (Души + Тотем)
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, 60, 0.8, 1.2, 0.8, 0.2); // Золотые искры тотема
            level.sendParticles(ParticleTypes.SOUL, px, py, pz, 40, 0.6, 1.0, 0.6, 0.1); // Синие души
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 20, 0.5, 0.5, 0.5, 0.05); // Синее пламя
            level.sendParticles(new DustParticleOptions(0x00FFFF, 2.0f), px, py, pz, 30, 0.8, 0.8, 0.8, 0.2); // Бирюзовый

            player.teleportTo(tx, standBp.getY(), tz);
            player.resetFallDistance();

            double nx = player.getX();
            double ny = player.getY() + 1.0;
            double nz = player.getZ();

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, nx, ny, nz, 60, 0.8, 1.2, 0.8, 0.2);
            level.sendParticles(ParticleTypes.SOUL, nx, ny, nz, 40, 0.6, 1.0, 0.6, 0.1);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, nx, ny, nz, 20, 0.5, 0.5, 0.5, 0.05);
            level.sendParticles(new DustParticleOptions(0x00FFFF, 2.0f), nx, ny, nz, 30, 0.8, 0.8, 0.8, 0.2);

            // ЗВУКИ СПАСЕНИЯ (Тотем, душа, телепорт)
            level.playSound(null, px, py, pz, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, px, py, pz, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.playSound(null, nx, ny, nz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.3f);
            level.playSound(null, nx, ny, nz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);

            player.sendOverlayMessage(Component.literal("§dПоследний шанс! §fТелепортация."));
            return;
        }
    }
}