package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HeroSyncPayload;

import java.util.List;

public class HeroPassiveAbility implements Ability {
    @Override public String getId() { return "hero_aura"; }
    @Override public String getDisplayName() { return "Слава героя"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    private static int computeAmplifier(int trades) {
        return Math.min(9, trades / 50);
    }

    @Override
    public String getDescription() {
        return "Постоянно даёт «Герой деревни». Каждые 50 сделок — +1 уровень. Максимум X.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int trades = net.sd1g.classes.data.AbilityClientState.heroTrades;
        int amp = computeAmplifier(trades);
        return List.of(
                "§7Эффект: §bГерой деревни " + toRoman(amp + 1),
                "§7Сделок: §f" + trades
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);

        // Синхронизация счётчика
        int currentStat;
        try {
            Stat<Identifier> stat = Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER);
            currentStat = player.getStats().getValue(stat);
        } catch (Exception e) {
            currentStat = data.heroTrades + data.heroTradeBaseline;
        }
        int delta = Math.max(0, currentStat - data.heroTradeBaseline);

        int oldAmp = computeAmplifier(data.heroTrades);
        int newAmp = computeAmplifier(delta);

        if (delta != data.heroTrades) {
            data.heroTrades = delta;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new HeroSyncPayload(delta));
        }

        MobEffectInstance current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != newAmp;
        boolean lowDuration = current != null && current.getDuration() < 40;
        boolean needsRefresh = wasAbsent || wrongAmp || lowDuration;

        // Повышение уровня — событийный звук + вспышка
        if (newAmp > oldAmp && !wasAbsent) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.5f);

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.6, 0.8, 0.6, 0.4);
            level.sendParticles(new DustParticleOptions(0xFFD91A, 1.8f), x, y, z, 30, 0.6, 0.6, 0.6, 0.15);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.3, z, 25, 0.8, 0.5, 0.8, 0.4);
        }

        if (needsRefresh) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.HERO_OF_THE_VILLAGE, 100, newAmp, true, false));
        }

        // Звук только при первой выдаче эффекта (был отсутствовал)
        if (wasAbsent) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();
            level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9f, 1.3f);
        }

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Зелёная пыль жителей вокруг — раз в 8 тиков
        if (player.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.5, z, 4, 0.6, 0.5, 0.6, 0.05);
        }
        // Золотая спираль — восходит, раз в 12 тиков
        if (player.tickCount % 12 == 0) {
            double angle = (player.tickCount * 0.3) % (Math.PI * 2);
            for (int i = 0; i < 3; i++) {
                double dy = 0.3 + i * 0.5;
                double r = 0.6 + i * 0.15;
                double px = x + Math.cos(angle + i) * r;
                double pz = z + Math.sin(angle + i) * r;
                level.sendParticles(new DustParticleOptions(0xFFD91A, 1.0f),
                        px, y + dy, pz, 1, 0, 0.02, 0, 0);
            }
        }
        // Искры опыта высоко над головой — раз в 20 тиков
        if (player.tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.6, z, 4, 0.6, 0.3, 0.6, 0.2);
        }
        // Редкие TOTEM_OF_UNDYING — «присутствие героя»
        if (player.tickCount % 60 == 0) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.2, z, 2, 0.4, 0.3, 0.4, 0.05);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}