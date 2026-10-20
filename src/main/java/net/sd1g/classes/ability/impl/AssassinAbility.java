package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class AssassinAbility implements Ability {
    @Override public String getId() { return "assassin"; }
    @Override public String getDisplayName() { return "Смертельный удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.005) * 100;
        return "С шансом " + String.format("%.1f", chance) + "% мгновенно убивает обычного моба, добавляя +" + cfg.getEffect("assassin_bonus_per_kill", 0.3) + " к базовому урону.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double bonus = AbilityClientState.assassinBonusDamage;
        return List.of(
                "§7Накопленный бонус: §c+" + String.format("%.1f", bonus) + " урона",
                "§7Прогресс сбрасывается при смене класса"
        );
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        if (target instanceof Player) return;
        if (!(target instanceof Enemy)) return;
        if (target.getMaxHealth() > 100) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("assassin_instakill_chance", 0.005);

        if (attacker.getRandom().nextDouble() < chance) {
            PureDamage.apply(attacker, target, 1000.0f);

            double bonus = cfg.getEffect("assassin_bonus_per_kill", 0.3);
            PlayerClassData data = PlayerClassData.get(attacker);
            data.assassinBonusDamage += bonus;
            PlayerClassData.markDirty(attacker);

            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    attacker,
                    new net.sd1g.classes.network.AssassinSyncPayload(data.assassinBonusDamage)
            );

            attacker.sendOverlayMessage(Component.literal("§cСмертельный удар! §f+ " + bonus + " к урону."));
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.assassinBonusDamage = 0.0;
        PlayerClassData.markDirty(player);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player,
                new net.sd1g.classes.network.AssassinSyncPayload(0.0)
        );
    }
}