package net.sd1g.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.sd1g.classes.ability.impl.BedExplosionTracker;
import net.sd1g.classes.ability.impl.EndCrystalTracker;
import net.sd1g.classes.ability.impl.TntOwnerTracker;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

/**
 * Уменьшает урон от взрывов ТНТ / кровати / кристаллов Энда,
 * если виновник взрыва — игрок со штрафным классом
 * (brute / mad_farmer). Применяется тот же множитель, что и
 * штраф за «неправильное» оружие.
 */
@Mixin(LivingEntity.class)
public class ExplosionDamageMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float classeshardcore$reduceExplosionDamage(float amount, ServerLevel level, DamageSource source) {
        if (amount <= 0) return amount;
        if (!source.is(DamageTypeTags.IS_EXPLOSION)) return amount;

        ServerPlayer responsible = findResponsiblePlayer(source, level);
        if (responsible == null) return amount;

        PlayerClassData data = PlayerClassData.get(responsible);
        ModConfig cfg = ModConfig.getInstance();

        if ("brute".equals(data.playerClass)) {
            double mult = cfg.getEffect("brute_other_damage_multiplier", 0.3);
            return (float) (amount * mult);
        }
        if ("mad_farmer".equals(data.playerClass)) {
            double mult = cfg.getEffect("farmer_other_damage_multiplier", 0.2);
            return (float) (amount * mult);
        }
        return amount;
    }

    private static ServerPlayer findResponsiblePlayer(DamageSource source, ServerLevel level) {
        Entity direct = source.getDirectEntity();

        // 1. ТНТ — через трекер поджигателя (UseBlockCallback → TntSpawnMixin)
        if (direct instanceof PrimedTnt tnt) {
            ServerPlayer owner = TntOwnerTracker.getOwner(tnt.getUUID(), level);
            if (owner != null) return owner;
        }

        // 2. Кристалл Энда — по трекеру атакующего (того, кто ударил)
        if (direct instanceof EndCrystal crystal) {
            ServerPlayer attacker = EndCrystalTracker.getAttacker(crystal.getUUID(), level);
            if (attacker != null) {
                EndCrystalTracker.remove(crystal.getUUID());
                return attacker;
            }
        }

        // 3. Кровать — по трекеру недавней попытки сна
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) {
            long now = level.getServer() != null ? level.getServer().getTickCount() : 0;
            UUID placerUuid = BedExplosionTracker.getRecentPlacer(now, 5);
            if (placerUuid != null && level.getServer() != null) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(placerUuid);
                if (sp != null) return sp;
            }
        }

        // 4. Всё остальное — через source.getEntity()
        if (source.getEntity() instanceof ServerPlayer sp) return sp;

        return null;
    }
}