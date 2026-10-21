package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class VampirePassiveAbility implements Ability {
    @Override public String getId() { return "vampire_lifesteal"; }
    @Override public String getDisplayName() { return "Жажда крови"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/vampire_lifesteal"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("vampire_passive_chance", 0.20) * 100);
        int percent = (int) Math.round(cfg.getEffect("vampire_passive_heal_percent", 0.60) * 100);
        return "С шансом " + chance + "% лечит на " + percent + "% урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("vampire_passive_chance", 0.20) * 100);
        return List.of("§7Шанс: §a" + chance + "%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks > 0) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("vampire_passive_chance", 0.20);
        if (attacker.getRandom().nextDouble() < chance) {
            double percent = cfg.getEffect("vampire_passive_heal_percent", 0.60);
            attacker.heal((float) (damageDealt * percent));

            ServerLevel level = attacker.level();

            // Кровавая аура вокруг врага
            level.sendParticles(new DustParticleOptions(0x8B0000, 1.5f), target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.02);

            // Хил на игроке
            level.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 0.5f, 0.5f);
        }
    }
}