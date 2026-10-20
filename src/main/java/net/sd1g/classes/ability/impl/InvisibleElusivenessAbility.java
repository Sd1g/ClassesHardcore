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
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.InvisibilitySyncPayload;

import java.util.List;

public class InvisibleElusivenessAbility implements Ability {
    @Override public String getId() { return "invisible_elusiveness"; }
    @Override public String getDisplayName() { return "Ускользание"; }
    @Override public boolean isPassive() { return true; }
    @Override public int getBaseCooldown() { return 0; }

    @Override
    public Identifier getIcon() { return ClassesHardcore.id("gui/sprites/ability/invisible_elusiveness"); }

    @Override
    public String getDescription() {
        return "При получении урона с вероятностью 2,5% становится невидимым на 5 секунд. Внутренний интервал 10 секунд.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Шанс: §a2.5%", "§7КД: §f10 сек.");
    }

    @Override
    public void onTakeDamage(ServerPlayer player, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;

        PlayerClassData data = PlayerClassData.get(player);
        long now = player.level().getGameTime();

        if (player.getRandom().nextDouble() < 0.025 && !data.isOnCooldown("invis_passive_cd", now)) {
            data.invisibilityActiveTicks = 100;
            data.invisibleAmbushArmed = true;
            PlayerClassData.markDirty(player);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
            ServerPlayNetworking.send(player, new InvisibilitySyncPayload(100));

            data.setCooldown("invis_passive_cd", now, 200); // 10 секунд
        }
    }
}