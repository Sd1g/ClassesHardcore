package net.sd1g.classes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.sd1g.classes.ability.impl.DodgerAbility;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.CooldownSyncPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class DodgerDodgeMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void classeshardcore$dodge(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof ServerPlayer sp)) return;
        if (amount <= 0) return;
        if (source.getEntity() == null) return;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        PlayerClassData data = PlayerClassData.get(sp);
        if (!"dodger".equals(data.playerClass)) return;

        ModConfig cfg = ModConfig.getInstance();
        double baseChance = cfg.getEffect("dodger_dodge_chance", 0.20);
        int killsPerStep = cfg.getIntEffect("dodger_kills_per_step", 300);
        double maxBonus = cfg.getEffect("dodger_max_bonus_chance", 0.10);

        double bonus = Math.min((data.dodgerDodgeCount / (double)killsPerStep) * 0.01, maxBonus);
        double totalChance = baseChance + bonus;

        if (sp.getRandom().nextDouble() >= totalChance) return;

        DodgerAbility.playDodgeEffect(sp);
        data.dodgerDodgeCount++;
        PlayerClassData.markDirty(sp);

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                sp, new CooldownSyncPayload("dodger_dodges", data.dodgerDodgeCount)
        );

        if (amount >= 2.0f) sp.sendOverlayMessage(Component.literal("§bУклонение!§f Урон от атаки избежан."));

        cir.setReturnValue(false); // Отменяем урон
    }
}