package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.transportation.NetworkNode;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import com.thepigcat.transportlib.client.transportation.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;

public record AddNextNodePayload(TransportNetwork<?> network, BlockPos nodePos, Direction direction,
                                 BlockPos nextPos) implements CustomPacketPayload {
    public static final Type<AddNextNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "add_next_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddNextNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetwork.STREAM_CODEC,
            AddNextNodePayload::network,
            BlockPos.STREAM_CODEC,
            AddNextNodePayload::nodePos,
            Direction.STREAM_CODEC,
            AddNextNodePayload::direction,
            BlockPos.STREAM_CODEC,
            AddNextNodePayload::nextPos,
            AddNextNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            NetworkNode<?> networkNode = ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).get(nextPos);
            ClientNodes.NODES.get(network).get(nodePos).addNext(direction, networkNode);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle AddNextNodePayload");
            return null;
        });
    }

}