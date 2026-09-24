package com.dyxiaojiazi.guardian_stone.item;

import com.dyxiaojiazi.guardian_stone.GuardianStoneMod;
import com.dyxiaojiazi.guardian_stone.network.GuardianStoneNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainGuardianStoneItem extends Item {
    private static final String TAG_OWNER_UUID = "owner_uuid";
    private static final String TAG_OWNER_NAME = "owner_name";

    /** 冷却时间（秒），同时供 GuardianStoneMod 中的 UseCooldown 组件使用 */
    public static final int COOLDOWN_SECONDS = 10;
    /** 冷却时间（游戏刻，1 秒 = 20 刻） */
    public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

    public MainGuardianStoneItem(Properties properties) {
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

        // 正在冷却中，直接返回
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // 无归属时绑定当前玩家
        if (!tag.contains(TAG_OWNER_UUID)) {
            setOwner(stack, serverPlayer);
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.bound", serverPlayer.getScoreboardName())
                            .withStyle(ChatFormatting.GREEN)
            );
            return InteractionResult.SUCCESS;
        }

        int[] uuidArray = tag.getIntArray(TAG_OWNER_UUID).orElse(new int[0]);
        if (uuidArray.length != 4) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.not_your_stone").withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }
        UUID ownerUuid = new UUID(
                ((long) uuidArray[0] << 32) | (uuidArray[1] & 0xFFFFFFFFL),
                ((long) uuidArray[2] << 32) | (uuidArray[3] & 0xFFFFFFFFL)
        );
        if (!ownerUuid.equals(serverPlayer.getUUID())) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.not_your_stone").withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }

        List<ServerPlayer> targets = findTargets(serverPlayer);
        if (targets.isEmpty()) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.no_target").withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }

        if (targets.size() == 1) {
            performTeleport(serverPlayer, targets.get(0), stack);
        } else {
            ServerPlayNetworking.send(serverPlayer, new GuardianStoneNetworking.OpenSelectScreenPayload(
                    targets.stream().map(ServerPlayer::getUUID).toList()
            ));
        }
        return InteractionResult.SUCCESS;
    }

    public static List<ServerPlayer> findTargets(ServerPlayer owner) {
        List<ServerPlayer> targets = new ArrayList<>();
        ServerLevel level = (ServerLevel) owner.level();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player == owner) continue;
            if (player.isDeadOrDying()) continue;
            if (hasChildStone(player, owner.getUUID())) {
                targets.add(player);
            }
        }
        return targets;
    }

    public static boolean hasChildStone(ServerPlayer player, UUID ownerUuid) {
        if (isValidChildStone(player.getOffhandItem(), ownerUuid)) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isValidChildStone(player.getInventory().getItem(i), ownerUuid)) return true;
        }
        return false;
    }

    private static boolean isValidChildStone(ItemStack stack, UUID ownerUuid) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ChildGuardianStoneItem)) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("owner_uuid")) return false;
        int[] arr = tag.getIntArray("owner_uuid").orElse(new int[0]);
        if (arr.length != 4) return false;
        UUID uuid = new UUID(
                ((long) arr[0] << 32) | (arr[1] & 0xFFFFFFFFL),
                ((long) arr[2] << 32) | (arr[3] & 0xFFFFFFFFL)
        );
        return uuid.equals(ownerUuid);
    }

    public static void performTeleport(ServerPlayer user, ServerPlayer target, ItemStack mainStone) {
        user.teleportTo(
                (ServerLevel) target.level(),
                target.getX(), target.getY(), target.getZ(),
                java.util.Set.of(), target.getYRot(), target.getXRot(), true
        );

        addBuffsToTarget(target);
        applyDebuffsToMobs(target);

        int currentDamage = mainStone.getOrDefault(DataComponents.DAMAGE, 0);
        int newDamage = currentDamage + 1;
        mainStone.set(DataComponents.DAMAGE, newDamage);

        // 触发原版物品栏冷却动画（30 秒 = 600 刻）
        user.getCooldowns().addCooldown(mainStone, COOLDOWN_TICKS);

        if (newDamage >= mainStone.getMaxDamage()) {
            user.getInventory().removeItem(mainStone);
            user.sendOverlayMessage(
                    Component.translatable("message.guardian_stone.broken").withStyle(ChatFormatting.RED)
            );
        }

        user.sendOverlayMessage(
                Component.translatable("message.guardian_stone.teleported", target.getName())
        );
    }

    private static void addBuffsToTarget(ServerPlayer target) {
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));  // 伤害吸收 III 级
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
        target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
    }

    private static void applyDebuffsToMobs(ServerPlayer target) {
        AABB area = new AABB(target.blockPosition()).inflate(10);
        List<LivingEntity> entities = target.level().getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : entities) {
            if (entity instanceof Mob && entity != target) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3));
            }
        }
    }

    public static void registerTooltip() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.is(GuardianStoneMod.MAIN_GUARDIAN_STONE)) return;
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(TAG_OWNER_UUID)) {
                String name = tag.getString(TAG_OWNER_NAME).orElse("未知");
                lines.add(Component.translatable("item.guardian_stone.main_guardian_stone.owner", name).withStyle(ChatFormatting.GRAY));
            }
        });
    }

    public static void setOwner(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID uuid = player.getUUID();
        tag.putIntArray(TAG_OWNER_UUID, new int[]{
                (int) (uuid.getMostSignificantBits() >> 32),
                (int) uuid.getMostSignificantBits(),
                (int) (uuid.getLeastSignificantBits() >> 32),
                (int) uuid.getLeastSignificantBits()
        });
        tag.putString(TAG_OWNER_NAME, player.getScoreboardName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}