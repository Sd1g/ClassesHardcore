package net.sd1g.classes.ability.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;

import java.util.List;

public class GluttonyAbility implements Ability {
    @Override public String getId() { return "slow_hunger"; }
    @Override public String getDisplayName() { return "Обжорство"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/slow_hunger"); }

    @Override
    public String getDescription() {
        double mult = ModConfig.getInstance().getEffect("gluttony_hunger_multiplier", 0.5);
        double times = mult > 0 ? 1.0 / mult : 1.0;
        String timesText = times == Math.floor(times) ? String.valueOf((int) times) : String.format("%.1f", times);
        return "Сытость тратится в " + timesText + " раз медленнее обычного.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        double mult = ModConfig.getInstance().getEffect("gluttony_hunger_multiplier", 0.5);
        double times = mult > 0 ? (1.0 / mult) : 1.0;
        String timesText = times == Math.floor(times) ? String.valueOf((int) times) : String.format("%.1f", times);
        return List.of("§7Расход сытости: §a" + timesText + "x §7медленнее");
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        ModConfig cfg = ModConfig.getInstance();
        int interval = cfg.getIntEffect("gluttony_burp_interval", 600);
        double chance = cfg.getEffect("gluttony_burp_chance", 0.15);

        // Случайная отрыжка
        if (player.tickCount % interval == 0) {
            if (player.getRandom().nextDouble() < chance) {
                ServerLevel level = player.level();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }
    }
}