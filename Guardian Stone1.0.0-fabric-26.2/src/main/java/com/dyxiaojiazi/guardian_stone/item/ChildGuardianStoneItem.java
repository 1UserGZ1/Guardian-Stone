package com.dyxiaojiazi.guardian_stone.item;

import com.dyxiaojiazi.guardian_stone.GuardianStoneMod;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ChildGuardianStoneItem extends Item {
    public ChildGuardianStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // 无归属时绑定当前玩家
        if (!tag.contains("owner_uuid")) {
            setOwner(stack, serverPlayer);
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.bound", serverPlayer.getScoreboardName())
                            .withStyle(ChatFormatting.GREEN)
            );
            return InteractionResult.SUCCESS;
        }

        // 已有归属则提示归属者
        String ownerName = tag.getString("owner_name").orElse("未知");
        serverPlayer.sendOverlayMessage(
                Component.translatable("item.guardian_stone.child_guardian_stone.owner", ownerName)
                        .withStyle(ChatFormatting.GRAY)
        );
        return InteractionResult.SUCCESS;
    }

    public static void registerTooltip() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.is(GuardianStoneMod.CHILD_GUARDIAN_STONE)) return;
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains("owner_uuid")) {
                String name = tag.getString("owner_name").orElse("未知");
                lines.add(Component.translatable("item.guardian_stone.child_guardian_stone.owner", name).withStyle(ChatFormatting.GRAY));
            }
        });
    }

    public static void setOwner(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID uuid = player.getUUID();
        tag.putIntArray("owner_uuid", new int[]{
                (int) (uuid.getMostSignificantBits() >> 32),
                (int) uuid.getMostSignificantBits(),
                (int) (uuid.getLeastSignificantBits() >> 32),
                (int) uuid.getLeastSignificantBits()
        });
        tag.putString("owner_name", player.getScoreboardName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}