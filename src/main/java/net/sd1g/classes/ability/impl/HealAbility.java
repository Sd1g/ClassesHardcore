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
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class HealAbility implements Ability {
    @Override public String getId() { return "heal"; }
    @Override public String getDisplayName() { return "Исцеление"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 440; } // 22 секунды

    private float getHealAmount(ServerPlayer player) {
        float base = 4.0f; // 4 HP
        PlayerClassData data = PlayerClassData.get(player);
        // Боевой ветеран: +0.5 HP за каждые 2000 убийств, макс +10 HP (20к убийств)
        float bonus = Math.min((data.warriorSwordKills / 2000) * 0.5f, 10.0f);
        return base + bonus;
    }

    @Override
    public String getDescription() {
        return "Восстанавливает 4 НР и накладывает Регенерацию I на 5 секунд. КД 22 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        if (player instanceof ServerPlayer sp) {
            float amount = getHealAmount(sp);
            return List.of(
                    "§7Мгновенное лечение: §a" + amount + " HP",
                    "§7Регенерация: §bI §7на §f5 сек.",
                    "§7Кулдаун: §f22 сек."
            );
        }
        return List.of();
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        float amount = getHealAmount(player);

        player.heal(amount);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0)); // Реген I, 5 сек

        ServerLevel level = player.level();
        double x = player.getX(), y = player.getY(), z = player.getZ();

        for (int i = 0; i < 12; i++) {
            level.sendParticles(ParticleTypes.HEART, x, y + 0.4 + i * 0.15, z, 1, 0.3, 0, 0.3, 0.0);
        }
        level.sendParticles(new DustParticleOptions(0xFFAA00, 1.4f), x, y + 0.5, z, 18, 0.5, 0.4, 0.5, 0.03);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.2, z, 8, 0.3, 0.3, 0.3, 0.02);

        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1f, 1.5f);

        player.sendOverlayMessage(Component.literal("§aИсцеление. §f+" + (int) amount + " HP."));
        return true;
    }
}