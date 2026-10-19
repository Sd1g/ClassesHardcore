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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.AssassinSyncPayload;

import java.util.List;
import java.util.Random;

public class AssassinAbility implements Ability {
    private static final Random RNG = new Random();
    public static final Identifier MODIFIER_ID = ClassesHardcore.id("assassin_bonus");

    @Override public String getId() { return "assassin_instakill"; }
    @Override public String getDisplayName() { return "Смертельный удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.003) * 100;
        return "С шансом " + String.format("%.1f", chance)
                + "% мгновенно убивает мобов, накапливая бонус к урону.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.003);
        double bonusPerKill = cfg.getEffect("assassin_bonus_per_kill", 0.2);
        double storedBonus = AbilityClientState.assassinBonusDamage;
        return List.of(
                "§7Шанс убийства: §c" + String.format("%.1f", chance * 100) + "%",
                "§7Бонус за убийство: §c+" + format(bonusPerKill),
                "§7Накоплено бонуса: §c+" + format(storedBonus)
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        applyBonusDamage(player);

        // Аура — тёмно-фиолетовые искры у ног
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (player.tickCount % 15 == 0) {
            level.sendParticles(new DustParticleOptions(0x4D0080, 0.9f),
                    x, y + 0.1, z, 2, 0.4, 0.05, 0.4, 0.02);
        }
        if (player.tickCount % 30 == 0) {
            for (int i = 0; i < 2; i++) {
                double dy = 0.4 + i * 0.6;
                level.sendParticles(new DustParticleOptions(0x4D0080, 0.7f),
                        x + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        y + dy,
                        z + (player.getRandom().nextDouble() - 0.5) * 0.5,
                        1, 0, 0.02, 0, 0);
            }
        }
        if (player.tickCount % 45 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, x, y + 0.8, z, 2, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private static boolean isInstakillable(LivingEntity target) {
        if (target instanceof Player) return false;
        if (!(target instanceof Mob)) return false;
        if (target instanceof WitherBoss) return false;
        if (target instanceof EnderDragon) return false;
        if (target instanceof Warden) return false;
        if (target instanceof ElderGuardian) return false;
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (!isInstakillable(target)) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.003);
        if (RNG.nextDouble() >= chance) return;

        ServerLevel level = attacker.level();
        target.hurtServer(level, attacker.damageSources().genericKill(), Float.MAX_VALUE);

        double bonus = cfg.getEffect("assassin_bonus_per_kill", 0.2);
        PlayerClassData data = PlayerClassData.get(attacker);
        data.assassinBonusDamage += bonus;
        PlayerClassData.markDirty(attacker);
        applyBonusDamage(attacker);
        ServerPlayNetworking.send(attacker, new AssassinSyncPayload(data.assassinBonusDamage));

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        // Инстакилл — большая вспышка
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 40, 0.6, 0.7, 0.6, 0.08);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 35, 0.7, 0.7, 0.7, 0.1);
        level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 20, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 25, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 20, 0.5, 0.5, 0.5, 0.08);
        level.sendParticles(new DustParticleOptions(0x4D0080, 2.0f), x, y, z, 40, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(new DustParticleOptions(0x991A1A, 1.8f), x, y, z, 30, 0.6, 0.6, 0.6, 0.12);

        level.playSound(null, x, y, z, SoundEvents.WARDEN_DEATH, SoundSource.PLAYERS, 1.5f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.ELDER_GUARDIAN_DEATH, SoundSource.PLAYERS, 1.3f, 1.8f);

        attacker.sendOverlayMessage(Component.literal(
                "§cКритический удар!§f +" + format(bonus) + " урона навсегда (§7итого: +"
                        + format(data.assassinBonusDamage) + "§f)"));
    }

    private static void applyBonusDamage(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        if (data.assassinBonusDamage <= 0) {
            attr.removeModifier(MODIFIER_ID);
            return;
        }
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                MODIFIER_ID, data.assassinBonusDamage, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void removeBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(MODIFIER_ID);
    }

    private static String format(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }
}