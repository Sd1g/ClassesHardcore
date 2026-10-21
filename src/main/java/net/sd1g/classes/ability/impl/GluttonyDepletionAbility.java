package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.ArmedStatePayload;

import java.util.List;

public class GluttonyDepletionAbility implements Ability {
    @Override public String getId() { return "depletion"; }
    @Override public String getDisplayName() { return "Истощение"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/depletion"); }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("depletion", 1000);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4) + 1;
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400) / 20;
        int cd = cfg.getCooldown("depletion", 1000) / 20;
        return "Следующий удар накладывает Голод " + amp + " на " + dur + " сек. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Голод V", "§7Длительность: §f20 сек.");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.gluttonyDepletionArmed = true;
        PlayerClassData.markDirty(player);

        ServerPlayNetworking.send(player, new ArmedStatePayload("depletion", true));

        ServerLevel level = player.level();

        // Эффект вокруг игрока (зеленые искры голода)
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.4, 0.4, 0.02);

        // Звук активации (выпивание зелья)
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 1.2f);

        player.sendOverlayMessage(Component.literal("§4Истощение активировано."));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.gluttonyDepletionArmed) return;
        data.gluttonyDepletionArmed = false;
        PlayerClassData.markDirty(attacker);

        ServerPlayNetworking.send(attacker, new ArmedStatePayload("depletion", false));

        ModConfig cfg = ModConfig.getInstance();
        int amp = cfg.getIntEffect("depletion_hunger_amplifier", 4);
        int dur = cfg.getIntEffect("depletion_hunger_duration_ticks", 400);

        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, amp, false, true));

        ServerLevel level = attacker.level();
        // Эффект попадания на цели
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}