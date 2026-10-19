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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class GluttonyDepletionAbility implements Ability {
    @Override public String getId() { return "depletion"; }
    @Override public String getDisplayName() { return "Истощение"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 1000; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400) / 20;
        int cdSec = cfg.getCooldown("depletion", getBaseCooldown()) / 20;
        return "Следующий удар накладывает Голод " + toRoman(amp) + " на " + dur + " сек. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400);
        int cd = cfg.getCooldown("depletion", getBaseCooldown());
        return List.of(
                "§7Эффект: §6Голод " + toRoman(amp),
                "§7Длительность: §f" + (dur / 20) + " сек.",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (!data.gluttonyDepletionArmed) return;

        // Аура «armed» — тёмная кровь вокруг рук
        if (player.tickCount % 10 == 0) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.1f),
                    x + 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.1f),
                    x - 0.4, y + 1.1, z, 2, 0.15, 0.1, 0.15, 0.02);
        }
        // Редкий WARDEN_HEARTBEAT «внутри» — визуально
        if (player.tickCount % 30 == 0) {
            ServerLevel level = player.level();
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    player.getX(), player.getY() + 1.3, player.getZ(),
                    1, 0.3, 0.1, 0.3, 0.02);
        }
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.gluttonyDepletionArmed = true;
        PlayerClassData.markDirty(player);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Тёмно-красный вихрь вокруг ног
        int points = 22;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.2;
            double rz = z + Math.sin(angle) * 1.2;
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.4f),
                    rx, y + 0.1, rz, 1, 0, 0.05, 0, 0);
        }
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 1.0, z, 15, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y + 1.0, z, 25, 0.6, 0.7, 0.6, 0.03);
        level.sendParticles(new DustParticleOptions(0x8B0000, 1.8f), x, y + 0.7, z, 25, 0.7, 0.7, 0.7, 0.1);
        level.sendParticles(new DustParticleOptions(0x4D0000, 1.5f), x, y + 1.2, z, 15, 0.6, 0.6, 0.6, 0.1);

        level.playSound(null, x, y, z, SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.PLAYERS, 1.3f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 1.2f, 1.1f);

        player.sendOverlayMessage(Component.literal("§4Истощение активировано. §fСледующий удар — Голод."));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.gluttonyDepletionArmed) return;
        data.gluttonyDepletionArmed = false;
        PlayerClassData.markDirty(attacker);

        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4);
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400);

        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, amp, false, true));

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 15, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(new DustParticleOptions(0x8B0000, 1.8f), x, y, z, 25, 0.5, 0.5, 0.5, 0.15);
        level.sendParticles(new DustParticleOptions(0x4D0000, 1.3f), x, y, z, 15, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 20, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, x, y, z, SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}