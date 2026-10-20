package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class VampirePassiveAbility implements Ability {
    @Override public String getId() { return "vampire_lifesteal"; }
    @Override public String getDisplayName() { return "Жажда крови"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/vampire_lifesteal"); }

    @Override
    public String getDescription() {
        return "Вне действия Вампиризма каждый удар с вероятностью 20% лечит на 60% фактически нанесённого урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §a20%", "§7Лечение: §a60% §7от урона");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.vampireActiveTicks > 0) return;

        if (attacker.getRandom().nextDouble() < 0.20) {
            attacker.heal(damageDealt * 0.60f);
            ServerLevel level = attacker.level();
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, target.getX(), target.getY() + 1, target.getZ(), 12, 0.5, 0.5, 0.5, 0.03);
        }
    }
}