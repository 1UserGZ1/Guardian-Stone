package com.dyxiaojiazi.guardian_stone.network;

import com.dyxiaojiazi.guardian_stone.GuardianStoneMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuardianStoneNetworking {

    public static final CustomPacketPayload.Type<OpenSelectScreenPayload> OPEN_SELECT_SCREEN =
            new CustomPacketPayload.Type<>(GuardianStoneMod.id("open_select_screen"));
    public static final CustomPacketPayload.Type<SelectTeleportPayload> SELECT_TELEPORT =
            new CustomPacketPayload.Type<>(GuardianStoneMod.id("select_teleport"));

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(OPEN_SELECT_SCREEN, OpenSelectScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SELECT_TELEPORT, SelectTeleportPayload.CODEC);
    }

    public record OpenSelectScreenPayload(List<UUID> targetUuids) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSelectScreenPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public OpenSelectScreenPayload decode(RegistryFriendlyByteBuf buf) {
                        int size = buf.readVarInt();
                        List<UUID> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readUUID());
                        }
                        return new OpenSelectScreenPayload(list);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, OpenSelectScreenPayload payload) {
                        buf.writeVarInt(payload.targetUuids().size());
                        for (UUID uuid : payload.targetUuids()) {
                            buf.writeUUID(uuid);
                        }
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return OPEN_SELECT_SCREEN;
        }
    }

    public record SelectTeleportPayload(UUID targetUuid) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectTeleportPayload> CODEC =
                new StreamCodec<>() {
                    @Override
                    public SelectTeleportPayload decode(RegistryFriendlyByteBuf buf) {
                        return new SelectTeleportPayload(buf.readUUID());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, SelectTeleportPayload payload) {
                        buf.writeUUID(payload.targetUuid());
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SELECT_TELEPORT;
        }
    }
}