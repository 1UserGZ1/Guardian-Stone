package com.dyxiaojiazi.guardian_stone.mixin;

import com.dyxiaojiazi.guardian_stone.item.ChildGuardianStoneItem;
import com.dyxiaojiazi.guardian_stone.item.MainGuardianStoneItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() instanceof MainGuardianStoneItem) {
            MainGuardianStoneItem.setOwner(stack, player);
        } else if (stack.getItem() instanceof ChildGuardianStoneItem) {
            ChildGuardianStoneItem.setOwner(stack, player);
        }
    }
}