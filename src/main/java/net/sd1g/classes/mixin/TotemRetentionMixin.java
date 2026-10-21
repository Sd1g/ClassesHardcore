package net.sd1g.classes.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.HudMessagePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(LivingEntity.class)
public class TotemRetentionMixin {

    private static final Random CLASSES_RNG = new Random();

    @Redirect(
            method = "checkTotemDeathProtection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            require = 0
    )
    private void classeshardcore$keepTotemShrink(ItemStack stack, int amount) {
        if (shouldKeepTotem(stack)) return;
        stack.shrink(amount);
    }

    @Redirect(
            method = "checkTotemDeathProtection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"),
            require = 0
    )
    private void classeshardcore$keepTotemConsume(ItemStack stack, int amount, LivingEntity entity) {
        if (shouldKeepTotem(stack)) return;
        stack.consume(amount, entity);
    }

    private boolean shouldKeepTotem(ItemStack stack) {
        if (!stack.is(Items.TOTEM_OF_UNDYING)) return false;
        if (!((Object) this instanceof ServerPlayer sp)) return false;

        PlayerClassData data = PlayerClassData.get(sp);
        if (!"reincarnator".equals(data.playerClass)) return false;

        ModConfig cfg = ModConfig.getInstance();
        int maxSaves = cfg.getIntEffect("reincarnator_max_consecutive_saves", 10);

        if (data.reincarnatorTotemSaves >= maxSaves) {
            data.reincarnatorTotemSaves = 0;
            PlayerClassData.markDirty(sp);
            return false;
        }

        double chance = cfg.getEffect("reincarnator_totem_save_chance", 0.70);
        if (CLASSES_RNG.nextDouble() >= chance) return false;

        data.reincarnatorTotemSaves++;
        PlayerClassData.markDirty(sp);

        ServerPlayNetworking.send(sp, new HudMessagePayload("§dРеинкарнация!", 0xFFAA00FF, 80));

        ServerLevel level = sp.level();
        double x = sp.getX();
        double y = sp.getY() + 1.0;
        double z = sp.getZ();

        // ==========================================
        // ЭПИЧНЫЙ ЭФФЕКТ ПЕРЕРОЖДЕНИЯ
        // ==========================================

        // 1. Столб света и золотые искры тотема
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 100, 0.5, 1.5, 0.5, 0.2);

        // 2. Зеленая магия жизни (возрождение)
        level.sendParticles(new DustParticleOptions(0x00FF00, 2.5f), x, y, z, 60, 0.8, 1.0, 0.8, 0.1);

        // 3. Вылетающие души (символ возвращения с того света)
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 40, 0.6, 1.2, 0.6, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 20, 0.5, 0.5, 0.5, 0.05);

        // 4. Белое сияние
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 50, 0.8, 1.5, 0.8, 0.1);

        // ==========================================
        // ЭПИЧНЫЕ ЗВУКИ ПЕРЕРОЖДЕНИЯ
        // ==========================================
        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);

        sp.sendOverlayMessage(Component.literal("§dРеинкарнация. §fТотем сохранён. §7(Сохранений подряд: " + data.reincarnatorTotemSaves + "/" + maxSaves + ")"));
        return true;
    }
}