package net.sd1g.classes.ability.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class DodgerAbility implements Ability {
    @Override public String getId() { return "dodger_passive"; }
    @Override public String getDisplayName() { return "Ловкость"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20) * 100);
        return "С шансом " + chance + "% полностью избегает урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Базовый шанс уклонения");
    }

    public static void playDodgeEffect(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
    }
}