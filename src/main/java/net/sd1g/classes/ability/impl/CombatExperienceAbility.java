package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class CombatExperienceAbility implements Ability {
    @Override public String getId() { return "combat_experience"; }
    @Override public String getDisplayName() { return "Боевой опыт"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_experience"); }

    @Override
    public String getDescription() {
        return "Наносит мечом на 15% больше итогового физического урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Бонус: §c+15% урона §7(только мечом)");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (damageDealt <= 0) return;

        if (attacker.getMainHandItem().is(ItemTags.SWORDS)) {
            ServerLevel level = attacker.level();

            double x = target.getX();
            double y = target.getY() + (target.getBbHeight() / 2.0);
            double z = target.getZ();

            // Огненно-кровавый взрыв частиц
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 2, 0.2, 0.2, 0.2, 0.0); // Взмах
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.3, 0.3, 0.3, 0.1); // Белые искры
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 15, 0.3, 0.3, 0.3, 0.05); // Огонь
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 8, 0.3, 0.3, 0.3, 0.05); // Капли лавы
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 5, 0.2, 0.2, 0.2, 0.1); // Красные брызги
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 6, 0.3, 0.3, 0.3, 0.02); // Синее пламя для красочности

            // Звук: критический удар + звук поджигания
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }
}