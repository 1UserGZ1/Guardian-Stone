package com.dyxiaojiazi.guardian_stone;

import com.dyxiaojiazi.guardian_stone.item.ChildGuardianStoneItem;
import com.dyxiaojiazi.guardian_stone.item.MainGuardianStoneItem;
import com.dyxiaojiazi.guardian_stone.network.GuardianStoneNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GuardianStoneMod implements ModInitializer {
	public static final String MOD_ID = "guardian_stone";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<Item> MAIN_GUARDIAN_STONE_KEY =
			ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "main_guardian_stone"));
	public static final ResourceKey<Item> CHILD_GUARDIAN_STONE_KEY =
			ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "child_guardian_stone"));

	public static final Item MAIN_GUARDIAN_STONE = new MainGuardianStoneItem(
			new Item.Properties()
					.stacksTo(1)
					.durability(10)
					.setId(MAIN_GUARDIAN_STONE_KEY)
	);

	public static final Item CHILD_GUARDIAN_STONE = new ChildGuardianStoneItem(
			new Item.Properties().stacksTo(1).setId(CHILD_GUARDIAN_STONE_KEY)
	);

	public static final CreativeModeTab GUARDIAN_STONE_TAB = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(MAIN_GUARDIAN_STONE))
			.title(Component.translatable("itemGroup.guardian_stone"))
			.displayItems((params, output) -> {
				output.accept(MAIN_GUARDIAN_STONE);
				output.accept(CHILD_GUARDIAN_STONE);
			})
			.build();

	@Override
	public void onInitialize() {
		GuardianStoneNetworking.registerPayloads();

		Registry.register(BuiltInRegistries.ITEM, MAIN_GUARDIAN_STONE_KEY, MAIN_GUARDIAN_STONE);
		Registry.register(BuiltInRegistries.ITEM, CHILD_GUARDIAN_STONE_KEY, CHILD_GUARDIAN_STONE);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("guardian_stone_tab"), GUARDIAN_STONE_TAB);

		// 服务端 C2S 接收器：处理玩家选择目标后的数据包
		ServerPlayNetworking.registerGlobalReceiver(
				GuardianStoneNetworking.SELECT_TELEPORT,
				(payload, context) -> context.server().execute(() -> {
					ServerPlayer player = context.player();
					// 通过 context.server() 获取服务器实例（player.server 是私有字段）
					ServerPlayer target = context.server().getPlayerList().getPlayer(payload.targetUuid());

					if (target == null || target.isDeadOrDying()) {
						player.sendOverlayMessage(
								Component.translatable("message.guardian_stone.no_target")
										.withStyle(ChatFormatting.RED)
						);
						return;
					}

					// 重新获取玩家手持的主护道石
					ItemStack stack = player.getMainHandItem();
					if (!(stack.getItem() instanceof MainGuardianStoneItem)) {
						stack = player.getOffhandItem();
					}
					if (stack.getItem() instanceof MainGuardianStoneItem) {
						MainGuardianStoneItem.performTeleport(player, target, stack);
					}
				})
		);

		LOGGER.info("Guardian Stone 模组已加载");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}