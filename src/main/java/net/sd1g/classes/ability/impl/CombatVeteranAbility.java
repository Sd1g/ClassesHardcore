package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.network.HudMessagePayload;

import java.util.List;

public class CombatVeteranAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_KILLS = new java.util.HashMap<>();

    @Override public String getId() { return "combat_veteran"; }
    @Override public String getDisplayName() { return "Боевой ветеран"; }
    @Override public boolean isPassive() { return true; }

    @Override public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_veteran"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int killsPerStep = cfg.getIntEffect("warrior_veteran_kills_per_step", 2000);
        float bonusPerStep = (float) cfg.getEffect("warrior_veteran_bonus_hp", 0.5);
        float maxBonus = (float) cfg.getEffect("warrior_veteran_max_bonus", 10.0);

        return "За каждые " + killsPerStep + " убийств мобов мечом эффективность способности «Исцеление» увеличивается на " + bonusPerStep + " HP. Максимальный бонус: +" + maxBonus + " HP.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int killsPerStep = cfg.getIntEffect("warrior_veteran_kills_per_step", 2000);
        float bonusPerStep = (float) cfg.getEffect("warrior_veteran_bonus_hp", 0.5);
        float maxBonus = (float) cfg.getEffect("warrior_veteran_max_bonus", 10.0);

        int kills = AbilityClientState.warriorSwordKills;
        float bonusHP = Math.min((kills / (float)killsPerStep) * bonusPerStep, maxBonus);

        int toNext = killsPerStep - (kills % killsPerStep);
        // Если достигнут максимум, сбрасываем счетчик "до следующего"
        if (kills >= (maxBonus / bonusPerStep) * killsPerStep) toNext = 0;

        return List.of(
                "§7Убийств мечом: §b" + kills,
                "§7Текущий бонус: §a+" + bonusHP + " HP",
                (toNext > 0 ? "§7До след. бонуса: §f" + toNext + " убийств" : "§7Максимальный бонус")
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int killsPerStep = cfg.getIntEffect("warrior_veteran_kills_per_step", 2000);

        int kills = AbilityClientState.warriorSwordKills;
        int last = LAST_KILLS.getOrDefault(player.getUUID(), 0);

        // Проверяем, достигли ли мы нового рубежа
        if (kills > last && kills % killsPerStep == 0) {
            ServerLevel level = player.level();

            ServerPlayNetworking.send(player, new HudMessagePayload("§6Боевой ветеран: Исцеление усилено!", 0xFFFFAA00, 80));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.3f, 1.5f);

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 100, 0.5, 1.5, 0.5, 0.2);
            level.sendParticles(ParticleTypes.FIREWORK, x, y + 1.0, z, 50, 1.0, 1.0, 1.0, 0.2);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 80, 1.5, 2.0, 1.5, 0.5);
            level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 40, 0.8, 1.2, 0.8, 0.1);

            for (int i = 0; i < 360; i += 20) {
                double rad = Math.toRadians(i);
                level.sendParticles(ParticleTypes.FLAME, x + Math.cos(rad) * 2.0, y + 0.2, z + Math.sin(rad) * 2.0, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }

        LAST_KILLS.put(player.getUUID(), kills);
    }
}