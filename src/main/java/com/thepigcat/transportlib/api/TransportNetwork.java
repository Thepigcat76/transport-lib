package com.thepigcat.transportlib.api;

import com.mojang.serialization.Codec;
import com.thepigcat.transportlib.TransportLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface TransportNetwork<T> {
    Codec<TransportNetwork<?>> CODEC = TransportLib.NETWORK_REGISTRY.byNameCodec();
    StreamCodec<? super RegistryFriendlyByteBuf, TransportNetwork<?>> STREAM_CODEC = ByteBufCodecs.INT.map(TransportLib.NETWORK_REGISTRY::byId, TransportLib.NETWORK_REGISTRY::getId);

    NetworkNodeImpl<T> createNode(BlockPos pos);

    void addConnection(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1);

    void removeConnection(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1);

    void addInteractor(ServerLevel serverLevel, BlockPos interactorPos);

    void removeInteractor(ServerLevel serverLevel, BlockPos interactorPos);

    boolean hasInteractorAt(ServerLevel serverLevel, BlockPos interactorPos);

    boolean checkForInteractorAt(ServerLevel serverLevel, BlockPos interactorPos, Direction direction);

    void addNode(ServerLevel level, BlockPos pos, NetworkNodeImpl<T> node);

    void addNodeAndUpdate(ServerLevel level, BlockPos pos, Direction[] connections, boolean dead, @Nullable BlockPos interactorPos, @Nullable Direction interactorConnection);

    NetworkNodeImpl<T> removeNode(ServerLevel serverLevel, BlockPos pos);

    NetworkNodeImpl<T> removeNodeAndUpdate(ServerLevel serverLevel, BlockPos pos);

    NetworkNodeImpl<T> getNodeAt(ServerLevel serverLevel, BlockPos pos);

    boolean hasNodeAt(ServerLevel serverLevel, BlockPos pos);

    T transport(ServerLevel serverLevel, BlockPos pos, T value, Direction ...directions);

    default @Nullable NetworkNodeImpl<T> findNextNode(@Nullable NetworkNodeImpl<T> selfNode, ServerLevel serverLevel, BlockPos pos, Direction direction) {
        return this.findNextNode(selfNode, serverLevel, pos, direction, Set.of());
    }

    @Nullable NetworkNodeImpl<T> findNextNode(@Nullable NetworkNodeImpl<T> selfNode, ServerLevel serverLevel, BlockPos pos, Direction direction, Set<BlockPos> ignoredNodes);

}
