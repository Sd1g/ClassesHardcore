package net.sd1g.classes.ability.impl;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.data.PlayerClassData;

import java.util.List;

public class ParkourAbility implements Ability {
    @Override public String getId() { return "parkour"; }
    @Override public String getDisplayName() { return "Паркур"; }
    @Override public boolean isPassive() { return false; }
    @Override public int getBaseCooldown() { return 600; } // 30 сек

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/parkour"); }

    @Override
    public String getDescription() {
        return "Даёт Прыгучесть V на 11 секунд. Перезарядка: 30 сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Эффект: §bПрыгучесть V", "§7Длительность: §f11 сек.", "§7Кулдаун: §f30 сек.");
    }

    @Override
    public int getDynamicCooldown(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        int bonusTicks = Math.min((data.stuntmanClimbedBlocks / 500) * 20, 100); // макс 5 сек
        return 600 - bonusTicks; // От 30 до 25 сек
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        int bonusTicks = Math.min((data.stuntmanClimbedBlocks / 500) * 20, 100);
        int duration = 220 + bonusTicks; // От 11 до 16 сек

        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 4, false, true, true)); // amp 4 = Jump V

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.3f, 1.6f);
        player.sendOverlayMessage(Component.literal("§eПаркур. §fПрыгучесть V на " + (duration / 20) + " сек."));
        return true;
    }
}