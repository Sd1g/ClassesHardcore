package net.sd1g.classes.ability.impl;

import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

/**
 * Пассивка «Ледяное ослабление».
 * Вся механика — в миксине {@link net.sd1g.classes.mixin.IceWeakeningMixin}.
 * Класс нужен только для отображения в HUD и тултипа.
 */
public class IceWeakeningAbility implements Ability {
    @Override public String getId() { return "ice_weakening"; }
    @Override public String getDisplayName() { return "Ледяное ослабление"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public String getDescription() {
        double weaken = ModConfig.getInstance().getEffect("ice_weakening_damage_per_level", 1.0);
        return "Каждый уровень эффекта «Замедление» на противнике снижает его исходящий урон на "
                + fmt(weaken) + " HP.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double weaken = ModConfig.getInstance().getEffect("ice_weakening_damage_per_level", 1.0);
        return List.of(
                "§7Ослабление: §c−" + fmt(weaken) + " §7урона за уровень Замедления",
                "§7Работает против атакующих Ледяного воина"
        );
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}