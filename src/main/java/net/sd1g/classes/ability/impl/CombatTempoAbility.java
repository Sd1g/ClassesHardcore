package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class CombatTempoAbility implements Ability {
    private static final Identifier MODIFIER_ID = ClassesHardcore.id("combat_tempo_buff");

    @Override public String getId() { return "combat_tempo"; }
    @Override public String getDisplayName() { return "Боевой темп"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("warrior_tempo_cooldown", 700);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("warrior_tempo_duration", 160) / 20;
        double mult = 1.0 + cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);
        int cd = cfg.getCooldown("warrior_tempo_cooldown", 700) / 20;
        return "На " + dur + " сек. ускоряет восстановление атаки в " + mult + " раза. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("warrior_tempo_duration", 160);
        double mult = 1.0 + cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);
        return List.of("§7Эффект: §bСкорость атаки x" + mult + " §7на §f" + (dur / 20) + " сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int duration = cfg.getIntEffect("warrior_tempo_duration", 160);
        double bonus = cfg.getEffect("warrior_tempo_attack_speed_bonus", 0.5);

        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            PlayerClassData data = PlayerClassData.get(player);
            data.setCooldown("combat_tempo_active", player.level().getGameTime(), duration);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        player.sendOverlayMessage(Component.literal("§eБоевой темп!"));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (!data.isOnCooldown("combat_tempo_active", player.level().getGameTime())) {
            AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attr != null && attr.getModifier(MODIFIER_ID) != null) attr.removeModifier(MODIFIER_ID);
        }
    }
}