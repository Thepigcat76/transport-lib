package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import com.thepigcat.transportlib.client.transportation.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;

public record RemoveNetworkNodePayload<T>(TransportNetwork<T> network, BlockPos pos) implements CustomPacketPayload {
    private static <T> RemoveNetworkNodePayload<T> untyped(TransportNetwork<?> network, BlockPos pos) {
        return new RemoveNetworkNodePayload<>((TransportNetwork<T>) network, pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type(network);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).remove(pos);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle remove network node payload", err);
            return null;
        });
    }

    public static <T> Type<RemoveNetworkNodePayload<T>> type(TransportNetwork<?> network) {
        ResourceLocation key = TransportLib.NETWORK_REGISTRY.getKey(network);
        return new Type<>(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "remove_%s_node".formatted(key.getPath())));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, RemoveNetworkNodePayload<T>> streamCodec(TransportNetwork<?> network) {
        return StreamCodec.composite(
                TransportNetwork.STREAM_CODEC,
                RemoveNetworkNodePayload::network,
                BlockPos.STREAM_CODEC,
                RemoveNetworkNodePayload::pos,
                RemoveNetworkNodePayload::untyped
        );
    }

}
