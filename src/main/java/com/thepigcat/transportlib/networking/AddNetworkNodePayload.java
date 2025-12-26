package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNodeImpl;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;

public record AddNetworkNodePayload<T>(TransportNetworkImpl<T> network, BlockPos pos,
                                       NetworkNodeImpl<T> node) implements CustomPacketPayload {
    private static <T> AddNetworkNodePayload<T> untyped(TransportNetworkImpl<?> network, BlockPos pos, NetworkNodeImpl<?> tNetworkNode) {
        return new AddNetworkNodePayload<>((TransportNetworkImpl<T>) network, pos, (NetworkNodeImpl<T>) tNetworkNode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type(network);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (node.uninitialized()) {
                node.initialize(ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()));
            }
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).put(pos, node);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle add network node payload", err);
            return null;
        });
    }

    public static <T> Type<AddNetworkNodePayload<T>> type(TransportNetworkImpl<?> network) {
        ResourceLocation key = TransportLib.NETWORK_REGISTRY.getKey(network);
        return new Type<>(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "add_%s_node".formatted(key.getPath())));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, AddNetworkNodePayload<T>> streamCodec(TransportNetworkImpl<?> network) {
        return StreamCodec.composite(
                TransportNetworkImpl.STREAM_CODEC,
                AddNetworkNodePayload::network,
                BlockPos.STREAM_CODEC,
                AddNetworkNodePayload::pos,
                NetworkNodeImpl.streamCodec((TransportNetworkImpl<T>) network),
                AddNetworkNodePayload::node,
                AddNetworkNodePayload::untyped
        );
    }

}