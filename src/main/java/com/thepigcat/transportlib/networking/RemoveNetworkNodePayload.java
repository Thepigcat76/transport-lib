package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;

public record RemoveNetworkNodePayload(TransportNetwork<?> network, BlockPos pos) implements CustomPacketPayload {
    public static final Type<RemoveNetworkNodePayload> TYPE = new Type<>(TransportLib.rl("remove_network_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNetworkNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetwork.STREAM_CODEC,
            RemoveNetworkNodePayload::network,
            BlockPos.STREAM_CODEC,
            RemoveNetworkNodePayload::pos,
            RemoveNetworkNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).remove(pos);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle remove network node payload", err);
            return null;
        });
    }

}
