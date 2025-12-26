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

import java.util.Map;

public record SyncNextNodePayload(TransportNetworkImpl<?> network) implements CustomPacketPayload {
    public static final Type<SyncNextNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "sync_next_nodes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNextNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetworkImpl.STREAM_CODEC,
            SyncNextNodePayload::network,
            SyncNextNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Map<BlockPos, NetworkNodeImpl<?>> nodes = ClientNodes.NODES.get(network);
            for (NetworkNodeImpl<?> node : nodes.values()) {
                if (node.uninitialized()) {
                    node.initialize(nodes);
                }
            }
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle RemoveNextNodePayload");
            return null;
        });
    }
}