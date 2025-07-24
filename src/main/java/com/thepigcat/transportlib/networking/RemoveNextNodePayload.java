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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public record RemoveNextNodePayload(TransportNetwork<?> network, BlockPos nodePos,
                                    Direction direction) implements CustomPacketPayload {
    public static final Type<RemoveNextNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "remove_next_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNextNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetwork.STREAM_CODEC,
            RemoveNextNodePayload::network,
            BlockPos.STREAM_CODEC,
            RemoveNextNodePayload::nodePos,
            Direction.STREAM_CODEC,
            RemoveNextNodePayload::direction,
            RemoveNextNodePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            NetworkNode<?> networkNode = ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).get(nodePos);
            if (networkNode != null) {
                networkNode.removeNext(direction);
            }
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle RemoveNextNodePayload");
            return null;
        });
    }
}