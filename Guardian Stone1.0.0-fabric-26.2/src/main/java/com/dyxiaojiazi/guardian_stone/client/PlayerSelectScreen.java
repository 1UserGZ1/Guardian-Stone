package com.dyxiaojiazi.guardian_stone.client;

import com.dyxiaojiazi.guardian_stone.network.GuardianStoneNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class PlayerSelectScreen extends Screen {
    private final List<UUID> targetUuids;

    public PlayerSelectScreen(List<UUID> targetUuids) {
        super(Component.translatable("gui.guardian_stone.select_player"));
        this.targetUuids = targetUuids;
    }

    @Override
    protected void init() {
        int y = 40;
        for (UUID uuid : targetUuids) {
            String name = "未知玩家";
            if (Minecraft.getInstance().getConnection() != null) {
                var info = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    name = info.getProfile().name();
                }
            }

            final UUID targetUuid = uuid;
            Button button = Button.builder(Component.literal(name), (btn) -> {
                ClientPlayNetworking.send(new GuardianStoneNetworking.SelectTeleportPayload(targetUuid));
                this.onClose();
            }).bounds(this.width / 2 - 100, y, 200, 20).build();

            this.addRenderableWidget(button);
            y += 24;
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.guardian_stone.cancel"), (btn) ->
                this.onClose()
        ).bounds(this.width / 2 - 100, y + 10, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 20, 0xFFFFFFFF, true);
    }
}