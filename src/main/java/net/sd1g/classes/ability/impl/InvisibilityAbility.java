package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.InvisibilitySyncPayload;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class InvisibilityAbility implements Ability {
    @Override public String getId() { return "invisibility"; }
    @Override public String getDisplayName() { return "Невидимость"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("invisibility", 1200);
    }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/invisibility"); }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int dur = cfg.getIntEffect("invisibility_duration_ticks", 100) / 20;
        int cd = cfg.getCooldown("invisibility", 1200) / 20;
        return "Невидимость на " + dur + " сек. Первый удар наносит +200% урона + 1 НР чистого урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Длительность: §f5 сек.", "§7Кулдаун: §f60 сек.");
    }

    @Override
    public boolean isHighlighted(Player player) {
        return AbilityClientState.invisibilityActiveTicks > 0;
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        int duration = ModConfig.getInstance().getIntEffect("invisibility_duration_ticks", 100);
        PlayerClassData data = PlayerClassData.get(player);
        data.invisibilityActiveTicks = duration;
        data.invisibleAmbushArmed = true;
        PlayerClassData.markDirty(player);

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
        ServerPlayNetworking.send(player, new InvisibilitySyncPayload(duration));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.3f, 1.2f);
        player.sendOverlayMessage(Component.literal("§bНевидимость на " + (duration/20) + " сек."));
        return true;
    }

    @Override
    public void onPassiveTick(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks <= 0) return;

        data.invisibilityActiveTicks--;

        if (data.invisibilityActiveTicks <= 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));
            player.sendOverlayMessage(Component.literal("§bНевидимость закончилась"));
        }
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (data.invisibilityActiveTicks > 0 && data.invisibleAmbushArmed) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            attacker.removeEffect(MobEffects.INVISIBILITY);
            attacker.setInvisible(false);
            PlayerClassData.markDirty(attacker);
            ServerPlayNetworking.send(attacker, new InvisibilitySyncPayload(0));

            float pureDmg = (float) ModConfig.getInstance().getEffect("ambush_pure_damage", 2.0);
            PureDamage.apply(attacker, target, pureDmg);

            attacker.sendOverlayMessage(Component.literal("§cВы раскрыты!"));
        }
    }

    public static void cancelIfActive(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        if (data.invisibilityActiveTicks > 0) {
            data.invisibilityActiveTicks = 0;
            data.invisibleAmbushArmed = false;
            player.removeEffect(MobEffects.INVISIBILITY);
            PlayerClassData.markDirty(player);
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));
        }
    }
}