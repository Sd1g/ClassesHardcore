package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;
import java.util.Random;

public class EarthshakerAbility implements Ability {
    private static final Random RNG = new Random();

    @Override public String getId() { return "earthshaker"; }
    @Override public String getDisplayName() { return "Дрожь земли"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 400; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        int cdSec = cfg.getCooldown("earthshaker", getBaseCooldown()) / 20;
        return "Следующий удар булавой подбрасывает цель на " + min + "–" + max
                + " блоков вверх и в случайную сторону. Кулдаун " + cdSec + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        int cd = cfg.getCooldown("earthshaker", getBaseCooldown());
        return List.of(
                "§7Подброс вверх: §f" + min + "–" + max + " блоков",
                "§7Только булава",
                "§7Кулдаун: §f" + (cd / 20) + " сек."
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (!data.bruteShakeArmed) return;

        // Аура «armed» — коричнево-золотая пыль у ног
        if (player.tickCount % 8 == 0) {
            ServerLevel level = player.level();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            level.sendParticles(new DustParticleOptions(0x8B5A2B, 1.1f),
                    x, y + 0.1, z, 3, 0.4, 0.05, 0.4, 0.02);
            level.sendParticles(new DustParticleOptions(0xFFAA00, 0.8f),
                    x, y + 0.2, z, 2, 0.4, 0.05, 0.4, 0.02);
        }
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.bruteShakeArmed = true;
        PlayerClassData.markDirty(player);

        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Коричневое кольцо у ног + золотые искры
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            double rx = x + Math.cos(angle) * 1.1;
            double rz = z + Math.sin(angle) * 1.1;
            level.sendParticles(new DustParticleOptions(0x8B5A2B, 1.3f),
                    rx, y + 0.05, rz, 1, 0, 0.05, 0, 0);
        }
        level.sendParticles(ParticleTypes.CRIT, x, y + 0.3, z, 15, 0.6, 0.2, 0.6, 0.1);
        level.sendParticles(new DustParticleOptions(0x8B5A2B, 1.8f), x, y + 0.5, z, 20, 0.7, 0.2, 0.7, 0.15);
        level.sendParticles(new DustParticleOptions(0xFFAA00, 1.5f), x, y + 0.8, z, 15, 0.6, 0.2, 0.6, 0.1);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y + 0.5, z, 10, 0.5, 0.2, 0.5, 0.1);

        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 1.3f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_NETHERITE,
                SoundSource.PLAYERS, 1.2f, 1.4f);

        player.sendOverlayMessage(Component.literal("§6Дрожь земли. §fСледующий удар булавой подбросит цель."));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.bruteShakeArmed) return;
        if (!attacker.getMainHandItem().is(Items.MACE)) return;

        data.bruteShakeArmed = false;
        PlayerClassData.markDirty(attacker);

        ModConfig cfg = ModConfig.getInstance();
        double minUp = cfg.getEffect("earthshaker_min_launch", 5.0);
        double maxUp = cfg.getEffect("earthshaker_max_launch", 10.0);
        double up = minUp + RNG.nextDouble() * (maxUp - minUp);

        double angle = RNG.nextDouble() * Math.PI * 2;
        double sideStrength = 0.5 + RNG.nextDouble() * 0.7;
        double vx = Math.cos(angle) * sideStrength;
        double vz = Math.sin(angle) * sideStrength;
        double vy = Math.sqrt(up) * 0.55;

        target.setDeltaMovement(vx, vy, vz);
        target.hurtMarked = true;
        target.setOnGround(false);

        ServerLevel level = attacker.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.3;
        double z = target.getZ();

        // Массовая вспышка
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 25, 0.6, 0.4, 0.6, 0.2);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 15, 0.7, 0.4, 0.7, 0.15);
        level.sendParticles(new DustParticleOptions(0x8B5A2B, 2.0f), x, y, z, 30, 0.7, 0.5, 0.7, 0.15);
        level.sendParticles(new DustParticleOptions(0xFFAA00, 1.5f), x, y + 0.5, z, 20, 0.6, 0.5, 0.6, 0.15);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.3f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 1.4f, 0.7f);
    }
}