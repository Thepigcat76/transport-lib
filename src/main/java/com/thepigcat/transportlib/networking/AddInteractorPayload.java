package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

public record AddInteractorPayload(TransportNetworkImpl<?> network, BlockPos interactorPos) implements CustomPacketPayload {
    public static final Type<AddInteractorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "add_interactor"));
    public static final StreamCodec<? super RegistryFriendlyByteBuf, AddInteractorPayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetworkImpl.STREAM_CODEC,
            AddInteractorPayload::network,
            BlockPos.STREAM_CODEC,
            AddInteractorPayload::interactorPos,
            AddInteractorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.INTERACTORS.computeIfAbsent(network, k -> new HashSet<>()).add(interactorPos);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle AddInteractor payload", err);
            return null;
        });
    }

}