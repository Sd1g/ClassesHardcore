package net.sd1g.classes.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.util.PureDamage;

import java.util.List;

public class EarthshakerAbility implements Ability {
    @Override public String getId() { return "earthshaker"; }
    @Override public String getDisplayName() { return "Дрожь земли"; }
    @Override public boolean isPassive() { return false; }

    @Override
    public int getBaseCooldown() {
        return ModConfig.getInstance().getCooldown("earthshaker", 500);
    }

    @Override
    public String getDescription() {
        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        double dmg = cfg.getEffect("earthshaker_pure_damage", 2.0) / 2.0;
        int cd = getBaseCooldown() / 20;
        return "Следующий удар подбрасывает врага на " + min + "-" + max + " блоков и наносит " + dmg + " НР чистого урона. Перезарядка: " + cd + " сек.";
    }

    @Override
    public List<String> getExtraTooltipLines(Player player) {
        return List.of("§7Работает только по мобам/игрокам");
    }

    @Override
    public boolean onActivate(ServerPlayer player) {
        PlayerClassData data = PlayerClassData.get(player);
        data.bruteShakeArmed = true;
        PlayerClassData.markDirty(player);
        player.sendOverlayMessage(Component.literal("§eДрожь земли готова!"));
        return true;
    }

    @Override
    public void onDealDamage(ServerPlayer attacker, LivingEntity target, float damageDealt) {
        PlayerClassData data = PlayerClassData.get(attacker);
        if (!data.bruteShakeArmed) return;

        data.bruteShakeArmed = false;
        PlayerClassData.markDirty(attacker);

        ModConfig cfg = ModConfig.getInstance();
        int min = (int) cfg.getEffect("earthshaker_min_launch", 5.0);
        int max = (int) cfg.getEffect("earthshaker_max_launch", 10.0);
        double dmg = cfg.getEffect("earthshaker_pure_damage", 2.0);

        int launch = min + attacker.getRandom().nextInt(Math.max(1, max - min + 1));

        target.setDeltaMovement(target.getDeltaMovement().x, launch * 0.1, target.getDeltaMovement().z);
        target.hurtMarked = true;

        PureDamage.apply(attacker, target, (float)dmg);

        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}