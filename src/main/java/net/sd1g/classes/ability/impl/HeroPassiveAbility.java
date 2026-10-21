package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HeroSyncPayload;

import java.util.ArrayList;
import java.util.List;

public class HeroPassiveAbility implements Ability {
    @Override public String getId() { return "hero_aura"; }
    @Override public String getDisplayName() { return "Слава героя"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/hero_aura"); }

    @Override
    public String getDescription() {
        return "Постоянно даёт «Герой деревни I». Каждые 50 сделок повышают уровень эффекта на 1 (максимум X).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int trades = AbilityClientState.heroTrades;
        int amp = Math.min(trades / 50, 9);
        int toNext = 50 - (trades % 50);
        if (trades >= 500) toNext = 0;

        List<String> lines = new ArrayList<>();
        lines.add("§7Эффект: §bГерой деревни " + toRoman(amp + 1));
        lines.add("§7Сделок: §f" + trades);

        if (toNext > 0) {
            lines.add("§7До след. уровня: §f" + toNext + " сделок");
        } else {
            lines.add("§7Максимальный уровень достигнут");
        }

        return lines;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);

        int currentStat;
        try {
            Stat<Identifier> stat = Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER);
            currentStat = player.getStats().getValue(stat);
        } catch (Exception e) {
            currentStat = data.heroTrades + data.heroTradeBaseline;
        }
        int delta = Math.max(0, currentStat - data.heroTradeBaseline);

        int newAmp = Math.min(delta / 50, 9);

        if (delta != data.heroTrades) {
            data.heroTrades = delta;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new HeroSyncPayload(delta));
        }

        MobEffectInstance current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        boolean wasAbsent = current == null;
        boolean wrongAmp = current != null && current.getAmplifier() != newAmp;

        if (wasAbsent || wrongAmp || (current != null && current.getDuration() < 40)) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 100, newAmp, true, false));
        }

        // Мягкая, но заметная аура героя
        if (player.tickCount % 6 == 0) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            // Кружащиеся зеленые искры и золотая пыль
            double angle = (player.tickCount * 0.15) % (Math.PI * 2);
            double radius = 0.8;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, y + (Math.sin(player.tickCount * 0.1) * 0.3), pz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(new DustParticleOptions(0xFFD700, 1.0f), x, y, z, 1, 0.4, 0.4, 0.4, 0.02);
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}