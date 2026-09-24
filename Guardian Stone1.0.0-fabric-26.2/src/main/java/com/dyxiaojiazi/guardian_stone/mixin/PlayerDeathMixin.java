package com.dyxiaojiazi.guardian_stone.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerDeathMixin {
	// 完整死亡不掉落需要配合 ServerPlayerEvents.COPY_FROM 或在 ServerPlayer#dropEquipment 中过滤物品，
	// 这里先占位，避免影响主功能编译通过。
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void onDropEquipment(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		// TODO: 过滤主护道石、子护道石
	}
}