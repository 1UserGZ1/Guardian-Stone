package com.dyxiaojiazi.guardian_stone.client;

import com.dyxiaojiazi.guardian_stone.item.ChildGuardianStoneItem;
import com.dyxiaojiazi.guardian_stone.item.MainGuardianStoneItem;
import com.dyxiaojiazi.guardian_stone.network.GuardianStoneNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class GuardianStoneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册物品悬浮提示
        MainGuardianStoneItem.registerTooltip();
        ChildGuardianStoneItem.registerTooltip();

        // 接收服务端请求打开 GUI 的数据包
        ClientPlayNetworking.registerGlobalReceiver(GuardianStoneNetworking.OPEN_SELECT_SCREEN,
                (payload, context) -> context.client().execute(() ->
                        // 26.2 中通过 gui 属性调用 setScreen
                        Minecraft.getInstance().gui.setScreen(new PlayerSelectScreen(payload.targetUuids()))
                ));
    }
}