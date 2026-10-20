package net.sd1g.classes.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;
import java.util.Random;

public class TeleporterLastChanceAbility implements Ability {
    @Override public String getId() { return "last_chance"; }
    @Override public String getDisplayName() { return "Последний шанс"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/last_chance"); }

    @Override
    public String getDescription() {
        return "При срабатывании Тотема бессмертия с шансом 30% переносит в случайную безопасную точку в радиусе 30 блоков.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §a30%", "§7Радиус: §f30 блоков");
    }

    // Вызывается из ClassesHardcore.java при активации тотема
    public static void tryTeleport(ServerPlayer player) {
        if (player.getRandom().nextDouble() >= 0.30) return;

        ServerLevel level = player.level();
        Random rng = new Random();

        for (int i = 0; i < 10; i++) { // 10 попыток найти место
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 5 + rng.nextDouble() * 25; // 5-30 блоков
            Vec3 target = player.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            BlockPos standBp = TeleporterAbility.findTargetPosition(level, target);
            if (standBp != null) {
                player.teleportTo(standBp.getX() + 0.5, standBp.getY(), standBp.getZ() + 0.5);
                player.resetFallDistance();

                level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§bПоследний шанс! §fТелепортация в безопасное место."));
                return;
            }
        }
    }
}