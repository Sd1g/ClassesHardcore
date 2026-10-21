package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
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

public class ArcherHunterMasteryAbility implements Ability {
    private static final java.util.Map<java.util.UUID, Integer> LAST_STEPS = new java.util.HashMap<>();

    @Override public String getId() { return "archer_hunter_mastery"; }
    @Override public String getDisplayName() { return "Мастерство охотника"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/archer_hunter_mastery"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int perStep = cfg.getIntEffect("archer_hunter_kills_per_step", 100);
        int maxRed = cfg.getIntEffect("archer_hunter_max_reduction_ticks", 200);
        return "За каждые " + perStep + " убийств из лука КД отпрыгивания сокращается на 1 сек (макс. " + (maxRed/20) + " сек).";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int kills = AbilityClientState.archerBowKills;
        int perStep = Math.max(1, cfg.getIntEffect("archer_hunter_kills_per_step", 100));
        int maxRed = cfg.getIntEffect("archer_hunter_max_reduction_ticks", 200);

        int reduction = Math.min((kills / perStep), maxRed / 20);
        int toNext = perStep - (kills % perStep);

        return List.of(
                "§7Убийств из лука: §b" + kills,
                "§7Сокращение КД: §a-" + reduction + " сек.",
                "§7До след. сокращения: §f" + toNext + " убийств"
        );
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int kills = AbilityClientState.archerBowKills;
        int perStep = Math.max(1, cfg.getIntEffect("archer_hunter_kills_per_step", 100));

        int currentStep = kills / perStep;
        int lastStep = LAST_STEPS.getOrDefault(player.getUUID(), 0);

        if (currentStep > lastStep) {
            ServerLevel level = player.level();
            ServerPlayNetworking.send(player, new HudMessagePayload("§aОхотник: КД уменьшено!", 0xFF55FF55, 80));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.5f);

            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 50, 0.8, 0.8, 0.8, 0.2);
            level.sendParticles(new DustParticleOptions(0x00FF00, 2.0f), x, y, z, 60, 1.0, 1.0, 1.0, 0.15);

            for (int i = 0; i < 30; i++) {
                // ИСПРАВЛЕНИЕ: level.random -> level.getRandom()
                level.sendParticles(ParticleTypes.ENCHANT,
                        x + (level.getRandom().nextDouble() - 0.5) * 3.0,
                        y + 2.0,
                        z + (level.getRandom().nextDouble() - 0.5) * 3.0,
                        1, 0.0, -0.5, 0.0, 0.1);
            }

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                level.sendParticles(new DustParticleOptions(0x00FF00, 1.5f),
                        x + Math.cos(rad) * 2.5, player.getY() + 0.2, z + Math.sin(rad) * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        LAST_STEPS.put(player.getUUID(), currentStep);
    }
}