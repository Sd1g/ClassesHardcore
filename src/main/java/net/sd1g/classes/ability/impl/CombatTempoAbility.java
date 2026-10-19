package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData; // ДОБАВЛЕН ИМПОРТ

import java.util.List;

public class CombatTempoAbility implements Ability {
    private static final Identifier MODIFIER_ID = ClassesHardcore.id("combat_tempo_buff");

    @Override public String getId() { return "combat_tempo"; }
    @Override public String getDisplayName() { return "Боевой темп"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 700; } // 35 секунд

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/combat_tempo"); }

    @Override
    public String getDescription() {
        return "На 8 секунд ускоряет восстановление атаки в 1,5 раза. Перезарядка: 35 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект: §bСкорость атаки x1.5 §7на §f8 сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

            PlayerClassData data = PlayerClassData.get(player);
            // Запоминаем, что бафф активен 8 секунд (160 тиков)
            data.setCooldown("combat_tempo_active", player.level().getGameTime(), 160);
        }

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        player.sendOverlayMessage(Component.literal("§eБоевой темп! §fСкорость атаки увеличена."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        // Если время баффа вышло, убираем модификатор
        if (!data.isOnCooldown("combat_tempo_active", now)) {
            AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attr != null && attr.getModifier(MODIFIER_ID) != null) {
                attr.removeModifier(MODIFIER_ID);
            }
        }
    }
}