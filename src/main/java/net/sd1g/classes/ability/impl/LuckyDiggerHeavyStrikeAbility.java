package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;

import java.util.List;

public class LuckyDiggerHeavyStrikeAbility implements Ability {
    @Override public String getId() { return "digger_heavy_strike"; }
    @Override public String getDisplayName() { return "Тяжёлый удар"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/digger_heavy_strike"); }

    @Override
    public String getDescription() {
        return "С вероятностью 5% удар накладывает Утомление V на 15 секунд.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §f5%");
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damage) {
        if (attacker.getRandom().nextDouble() < 0.05) {
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 300, 4, true, true));
            attacker.sendOverlayMessage(Component.literal("§6Тяжёлый удар! §fУтомление V."));
        }
    }
}