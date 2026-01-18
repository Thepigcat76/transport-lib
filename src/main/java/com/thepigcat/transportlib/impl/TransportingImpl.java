package com.thepigcat.transportlib.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.api.Transporting;
import com.thepigcat.transportlib.api.cache.NetworkRoute;
import com.thepigcat.transportlib.networking.SetNodeValuePayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

// Possible give helpers to easily expose this on the cable using
// block capabilities
public class TransportingImpl<T> implements Transporting<T> {
    private final TransportNetwork<T> network;
    private @Nullable T value;
    private NetworkRoute<T> route;

    public TransportingImpl(TransportNetwork<T> network) {
        this.network = network;
    }

    public void setRoute(NetworkRoute<T> route) {
        this.route = route;
    }

    @Override
    public NetworkRoute<T> getRoute() {
        return this.route;
    }

    @Override
    public void setValue(@Nullable T value) {
        this.value = value;
    }

    @Override
    public void trySyncValue(BlockPos pos) {
        if (this.network.isSynced()) {
            DataResult<Tag> result = this.network.getTransportingHandler().valueCodec().encodeStart(NbtOps.INSTANCE, this.getValue());
            if (result.isSuccess()) {
                PacketDistributor.sendToAllPlayers(new SetNodeValuePayload(result.getOrThrow(), pos));
            }
        }
    }

    @Override
    public @Nullable T getValue() {
        return value != null ? value : network.getTransportingHandler().defaultValue();
    }

    @Override
    public T removeValue() {
        T valueCopy = this.value;
        this.value = null;
        return valueCopy;
    }

    @Override
    public TransportNetwork<T> getNetwork() {
        return network;
    }

    private static <T> TransportingImpl<T> fromValue(TransportNetwork<?> network, T value) {
        TransportingImpl<T> transporting = new TransportingImpl<>((TransportNetwork<T>) network);
        transporting.setValue(value);
        return transporting;
    }

    public static <T> Codec<TransportingImpl<T>> codec(Codec<T> valueCodec) {
        return RecordCodecBuilder.create(inst -> inst.group(
                TransportNetwork.CODEC.fieldOf("network").forGetter(TransportingImpl::getNetwork),
                valueCodec.fieldOf("value").forGetter(TransportingImpl::getValue)
        ).apply(inst, TransportingImpl::fromValue));
    }

    public static <T> StreamCodec<? super RegistryFriendlyByteBuf, TransportingImpl<T>> streamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return StreamCodec.composite(
                TransportNetwork.STREAM_CODEC,
                TransportingImpl::getNetwork,
                codec,
                TransportingImpl::getValue,
                TransportingImpl::fromValue
        );
    }
}