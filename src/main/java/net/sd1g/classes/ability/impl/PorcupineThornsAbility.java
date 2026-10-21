package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class PorcupineThornsAbility implements Ability {
    @Override public String getId() { return "porcupine_thorns"; }
    @Override public String getDisplayName() { return "Игольчатый барьер"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/porcupine_thorns"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int chance = (int) Math.round(cfg.getEffect("porcupine_thorns_chance", 0.05) * 100);
        int percent = (int) Math.round(cfg.getEffect("porcupine_thorns_percent", 0.50) * 100);
        return "С шансом " + chance + "% возвращает " + percent + "% урона + 1 НР чистого.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает только в ближнем бою");
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        ModConfig cfg = ModConfig.getInstance();
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (data.isOnCooldown("porcupine_thorns_cd", now)) return;
        if (player.getRandom().nextDouble() >= cfg.getEffect("porcupine_thorns_chance", 0.05)) return;

        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            // ИСПРАВЛЕНО: Радиус из конфига (по умолчанию 4.0)
            double range = cfg.getEffect("porcupine_thorns_range", 4.0);
            if (attacker.distanceToSqr(player) <= range * range) {
                float percentDmg = damageTaken * (float) cfg.getEffect("porcupine_thorns_percent", 0.50);
                // ИСПРАВЛЕНО: Опечатка в ключе _dmg -> _damage
                float pureDmg = (float) cfg.getEffect("porcupine_thorns_pure_damage", 2.0);

                PureDamage.apply(player, attacker, percentDmg + pureDmg);

                // ИСПРАВЛЕНО: Кулдаун из конфига (по умолчанию 20)
                int cdTicks = cfg.getIntEffect("porcupine_thorns_internal_cooldown_ticks", 20);
                data.setCooldown("porcupine_thorns_cd", now, cdTicks);

                ServerLevel level = player.level();

                Vec3 start = new Vec3(player.getX(), player.getY() + 1.0, player.getZ());
                Vec3 end = new Vec3(attacker.getX(), attacker.getY() + (attacker.getBbHeight() / 2.0), attacker.getZ());
                Vec3 dir = end.subtract(start);

                for (double i = 0; i <= 1.0; i += 0.1) {
                    Vec3 p = start.add(dir.scale(i));
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }

                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }
    }
}