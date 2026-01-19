package com.thepigcat.transportlib.networking;

import com.mojang.serialization.Codec;
import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.client.ClientNodes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ClearClientCachePayload implements CustomPacketPayload {
    public static final Type<ClearClientCachePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TransportLib.MODID, "clear_client_cache"));
    public static final ClearClientCachePayload INSTANCE = new ClearClientCachePayload();
    public static final StreamCodec<? super RegistryFriendlyByteBuf, ClearClientCachePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClearClientCachePayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNodes.NODES.clear();
            ClientNodes.INTERACTORS.clear();
        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle ClearClientCachePayload");
            return null;
        });
    }

}
