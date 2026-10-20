package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class PorcupineThornsAbility implements Ability {
    @Override public String getId() { return "porcupine_thorns"; }
    @Override public String getDisplayName() { return "Игольчатый барьер"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

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
            if (attacker.distanceToSqr(player) <= 16.0) {
                float percentDmg = damageTaken * (float) cfg.getEffect("porcupine_thorns_percent", 0.50);
                float pureDmg = (float) cfg.getEffect("porcupine_thorns_pure_damage", 2.0);

                PureDamage.apply(player, attacker, percentDmg + pureDmg);
                data.setCooldown("porcupine_thorns_cd", now, 20);
            }
        }
    }
}