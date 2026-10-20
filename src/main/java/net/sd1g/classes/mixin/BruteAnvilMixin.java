package net.sd1g.classes.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.sd1g.classes.data.PlayerClassData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public class BruteAnvilMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void classeshardcore$restrictMaceEnchant(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack result = menu.getSlot(2).getItem();

        if (result.is(Items.MACE)) {
            // Используем аксессор для получения игрока
            Player player = ((ItemCombinerMenuAccessor) menu).getPlayer();

            if (player instanceof ServerPlayer sp) {
                if (!"brute".equals(PlayerClassData.get(sp).playerClass)) {
                    ItemEnchantments enchants = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    boolean hasHighEnchant = false;

                    for (var entry : enchants.entrySet()) {
                        if (entry.getIntValue() > 1) {
                            hasHighEnchant = true;
                            break;
                        }
                    }

                    if (hasHighEnchant) {
                        menu.getSlot(2).set(ItemStack.EMPTY);
                        sp.sendOverlayMessage(Component.literal("§cТолько Громила может зачаровывать булаву выше 1 уровня!"));
                    }
                }
            }
        }
    }
}