package com.thepigcat.transportlib.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.thepigcat.transportlib.TransportLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface TransportNetwork<T> {
    Codec<TransportNetwork<?>> CODEC = TransportLib.NETWORK_REGISTRY.byNameCodec();
    StreamCodec<? super RegistryFriendlyByteBuf, TransportNetwork<?>> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(TransportLib.NETWORK_REGISTRY::get, TransportLib.NETWORK_REGISTRY::getKey);

    NetworkNode<T> createNode(BlockPos pos);

    MapCodec<? extends NetworkNode<?>> getNodeCodec();

    Transporting<T> createTransporting();

    void connect(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1);

    void disconnect(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1);

    void addInteractor(ServerLevel serverLevel, BlockPos interactorPos);

    void removeInteractor(ServerLevel serverLevel, BlockPos interactorPos);

    boolean hasInteractorAt(ServerLevel serverLevel, BlockPos interactorPos);

    boolean checkForInteractorAt(ServerLevel serverLevel, BlockPos interactorPos, Direction direction);

    void addNode(NetworkNode<T> node, ServerLevel level, BlockPos pos, Direction[] connections, boolean dead);

    default void addNode(ServerLevel level, BlockPos pos, Direction[] connections, boolean dead) {
        this.addNode(this.createNode(pos), level, pos, connections, dead);
    }

    @Nullable NetworkNode<T> removeNode(ServerLevel serverLevel, BlockPos pos);

    @Nullable NetworkNode<T> getNodeAt(ServerLevel serverLevel, BlockPos pos);

    boolean hasNodeAt(ServerLevel serverLevel, BlockPos pos);

    T transport(ServerLevel serverLevel, BlockPos pos, T value, boolean simulate, Direction ...directions);

    default T transport(ServerLevel serverLevel, BlockPos pos, T value, Direction ...directions) {
        return this.transport(serverLevel, pos, value, false, directions);
    }

    default @Nullable NetworkNode<T> findNextNode(@Nullable NetworkNode<T> selfNode, ServerLevel serverLevel, BlockPos pos, Direction direction) {
        return this.findNextNode(selfNode, serverLevel, pos, direction, Set.of());
    }

    @Nullable NetworkNode<T> findNextNode(@Nullable NetworkNode<T> selfNode, ServerLevel serverLevel, BlockPos pos, Direction direction, Set<BlockPos> ignoredNodes);

    boolean isSynced();

    TransportingHandler<T> getTransportingHandler();

}
