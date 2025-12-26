package com.thepigcat.transportlib.networking;

import com.thepigcat.transportlib.TransportLib;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetNodeValuePayload(Tag nbt, BlockPos nodePos) implements CustomPacketPayload {
    public static final Type<SetNodeValuePayload> TYPE = new Type<>(TransportLib.rl("set_node_value"));
    public static final StreamCodec<? super RegistryFriendlyByteBuf, SetNodeValuePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.TAG,
            SetNodeValuePayload::nbt,
            BlockPos.STREAM_CODEC,
            SetNodeValuePayload::nodePos,
            SetNodeValuePayload::new
    );

    @Override
    public @NotNull Type<SetNodeValuePayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {

        }).exceptionally(err -> {
            TransportLib.LOGGER.error("Failed to handle SetNodeValuePayload");
            return null;
        });
    }

}
