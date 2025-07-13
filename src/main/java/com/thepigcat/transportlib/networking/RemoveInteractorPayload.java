package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import com.thepigcat.transportlib.client.transportation.ClientNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

public record RemoveInteractorPayload(TransportNetwork<?> network, BlockPos interactorPos) implements CustomPacketPayload {
    public static final Type<RemoveInteractorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "remove_interactor"));
    public static final StreamCodec<? super RegistryFriendlyByteBuf, RemoveInteractorPayload> STREAM_CODEC = StreamCodec.composite(
            TransportNetwork.STREAM_CODEC,
            RemoveInteractorPayload::network,
            BlockPos.STREAM_CODEC,
            RemoveInteractorPayload::interactorPos,
            RemoveInteractorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.INTERACTORS.computeIfAbsent(network, k -> new HashSet<>()).remove(interactorPos);
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle RemoveInteractor payload", err);
            return null;
        });
    }

}