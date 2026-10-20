package net.sd1g.classes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.PlayerClassData;
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

        // Если уже сохранили 10 раз подряд - тратим тотем
        if (data.reincarnatorTotemSaves >= 10) {
            data.reincarnatorTotemSaves = 0; // сбрасываем счетчик
            PlayerClassData.markDirty(sp);
            return false;
        }

        double chance = ModConfig.getInstance().getEffect("reincarnator_totem_save_chance", 0.70);
        if (CLASSES_RNG.nextDouble() >= chance) return false;

        // Сохраняем тотем и увеличиваем счетчик
        data.reincarnatorTotemSaves++;
        PlayerClassData.markDirty(sp);
        sp.sendOverlayMessage(Component.literal("§dРеинкарнация. §fТотем сохранён. §7(Сохранений подряд: " + data.reincarnatorTotemSaves + "/10)"));
        return true;
    }
}