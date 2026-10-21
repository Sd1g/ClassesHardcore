package net.sd1g.classes.ability.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.InvisibilitySyncPayload;

import java.util.List;

public class InvisibleElusivenessAbility implements Ability {
    @Override public String getId() { return "invisible_elusiveness"; }
    @Override public String getDisplayName() { return "Ускользание"; }
    @Override public boolean isPassive() { return true; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/invisible_elusiveness"); }

    @Override
    public String getDescription() {
        int chance = (int) Math.round(ModConfig.getInstance().getEffect("invisible_passive_invis_chance", 0.025) * 100);
        return "С шансом " + chance + "% уйти в инвиз при получении урона.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7КД: §f10 сек.");
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        ModConfig cfg = ModConfig.getInstance();
        double chance = cfg.getEffect("invisible_passive_invis_chance", 0.025);
        int cdTicks = cfg.getIntEffect("invisible_passive_invis_cooldown", 200);
        int duration = cfg.getIntEffect("invisibility_duration_ticks", 100);

        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (player.getRandom().nextDouble() < chance && !data.isOnCooldown("invis_passive_cd", now)) {
            data.invisibilityActiveTicks = duration;
            data.invisibleAmbushArmed = true;
            PlayerClassData.markDirty(player);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(duration));
            data.setCooldown("invis_passive_cd", now, cdTicks);

            // ИСПОЛЬЗУЕМ ТОТ ЖЕ МЕТОД, ЧТО И У АКТИВКИ (эффект исчезновения)
            InvisibilityAbility.spawnInvisibilityEffect(player.level(), player, false);
        }
    }
}