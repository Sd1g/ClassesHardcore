package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class HealingBrewAbility implements Ability {
    @Override public String getId() { return "healing_brew"; }
    @Override public String getDisplayName() { return "Лечебный отвар"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; }

    @Override
    public String getDescription() {
        int cdSec = ModConfig.getInstance().getCooldown("healing_brew", getBaseCooldown()) / 20;
        return "Мгновенно снимает все негативные эффекты. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int cd = ModConfig.getInstance().getCooldown("healing_brew", getBaseCooldown());
        return List.of("§7Кулдаун: §f" + (cd / 20) + " сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        player.getActiveEffects().stream()
                .filter(e -> !e.getEffect().value().isBeneficial())
                .map(e -> e.getEffect())
                .toList()
                .forEach(player::removeEffect);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Зелёно-золотой вихрь
        int points = 22;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            int color = (i % 2 == 0) ? 0x33FF33 : 0xD9A400;
            level.sendParticles(new DustParticleOptions(color, 1.5f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        for (int i = 0; i < 6; i++) {
            level.sendParticles(new DustParticleOptions(0x33FF33, 1.2f),
                    x, y + 0.3 + i * 0.25, z, 1, 0.15, 0, 0.15, 0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 25, 0.6, 0.6, 0.6, 0.08);
        level.sendParticles(ParticleTypes.HEART, x, y + 1.2, z, 6, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(new DustParticleOptions(0x33FF33, 1.8f), x, y + 1.0, z, 20, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0xD9A400, 1.5f), x, y + 1.4, z, 15, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.2, z, 10, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 1.5f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.2f, 1.5f);

        player.sendOverlayMessage(Component.literal("§aЛечебный отвар. §fНегативные эффекты сняты."));
        return true;
    }
}