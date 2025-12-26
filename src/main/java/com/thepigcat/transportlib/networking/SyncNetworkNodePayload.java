package com.thepigcat.transportlib.networking;


import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNodeImpl;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record SyncNetworkNodePayload<T>(TransportNetworkImpl<T> network, HashMap<BlockPos, NetworkNodeImpl<T>> nodes) implements CustomPacketPayload {
    public static <T> SyncNetworkNodePayload<T> untyped(TransportNetworkImpl<?> network, HashMap<BlockPos, NetworkNodeImpl<T>> nodes) {
        return new SyncNetworkNodePayload<>((TransportNetworkImpl<T>) network, nodes);
    }

    public static <T> SyncNetworkNodePayload<T> untyped(TransportNetworkImpl<?> network, Map<BlockPos, NetworkNodeImpl<T>> nodes) {
        return new SyncNetworkNodePayload<>((TransportNetworkImpl<T>) network, new HashMap<>(nodes));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type(network);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).putAll(this.nodes);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle sync network nodes payload", err);
            return null;
        });
    }

    public static <T> Type<SyncNetworkNodePayload<T>> type(TransportNetworkImpl<?> network) {
        ResourceLocation key = TransportLib.NETWORK_REGISTRY.getKey(network);
        return new Type<>(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "sync_%s_nodes".formatted(key.getPath())));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, SyncNetworkNodePayload<T>> streamCodec(TransportNetworkImpl<?> network) {
        return StreamCodec.composite(
                TransportNetworkImpl.STREAM_CODEC,
                SyncNetworkNodePayload::network,
                ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, NetworkNodeImpl.streamCodec((TransportNetworkImpl<T>) network)),
                SyncNetworkNodePayload::nodes,
                SyncNetworkNodePayload::untyped
        );
    }

}
