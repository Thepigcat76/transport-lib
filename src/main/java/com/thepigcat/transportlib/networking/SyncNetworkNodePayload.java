package com.thepigcat.transportlib.networking;


import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record SyncNetworkNodePayload(TransportNetwork<?> network, HashMap<BlockPos, Tag> nodes) implements CustomPacketPayload {
    public static final Type<SyncNetworkNodePayload> TYPE = new Type<>(TransportLib.rl("sync_network_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNetworkNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetwork.STREAM_CODEC,
            SyncNetworkNodePayload::network,
            ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.TAG),
            SyncNetworkNodePayload::nodes,
            SyncNetworkNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Map<BlockPos, NetworkNode<?>> nodes = this.nodes.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), NetworkNode.CODEC.decode(NbtOps.INSTANCE, entry.getValue()).getOrThrow().getFirst()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).putAll(nodes);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle sync network nodes payload", err);
            return null;
        });
    }

}
