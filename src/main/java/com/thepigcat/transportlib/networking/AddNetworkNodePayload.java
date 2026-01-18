package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record AddNetworkNodePayload(TransportNetwork<?> network, BlockPos pos,
                                    Tag encodedNode) implements CustomPacketPayload {
    public static final Type<AddNetworkNodePayload> TYPE = new Type<>(TransportLib.rl("add_network_node"));
    public static final StreamCodec<? super RegistryFriendlyByteBuf, AddNetworkNodePayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetworkImpl.STREAM_CODEC,
            AddNetworkNodePayload::network,
            BlockPos.STREAM_CODEC,
            AddNetworkNodePayload::pos,
            ByteBufCodecs.TAG,
            AddNetworkNodePayload::encodedNode,
            AddNetworkNodePayload::new
    );

    public static <T> AddNetworkNodePayload encodeNode(TransportNetwork<T> network, BlockPos pos, NetworkNodeImpl<T> node) {
        return new AddNetworkNodePayload(network, pos, NetworkNode.CODEC.encodeStart(NbtOps.INSTANCE, node).getOrThrow());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            NetworkNode<?> node = NetworkNode.CODEC.decode(NbtOps.INSTANCE, this.encodedNode()).getOrThrow().getFirst();
            if (node.isUninitialized()) {
                node.initialize((Map) ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()));
            }
            ClientNodes.NODES.computeIfAbsent(network, k -> new HashMap<>()).put(pos, node);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle add network node payload", err);
            return null;
        });
    }

}