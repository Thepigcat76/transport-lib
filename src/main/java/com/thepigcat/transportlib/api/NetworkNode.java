package com.thepigcat.transportlib.api;

import com.mojang.serialization.Codec;
import com.thepigcat.transportlib.TransportLib;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

public interface NetworkNode<T> {
    Codec<NetworkNode<?>> CODEC = TransportLib.NETWORK_REGISTRY.byNameCodec().dispatch(NetworkNode::getNetwork, TransportNetwork::getNodeCodec);
    StreamCodec<ByteBuf, NetworkNode<?>> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    void initialize(Map<BlockPos, NetworkNode<T>> nodes);

    void tick();

    List<Transporting<T>> getTransporting();

    void onConnect(ServerLevel serverLevel, BlockPos pos, Direction direction);

    void onDisconnect(ServerLevel serverLevel, BlockPos pos, Direction direction);

    boolean isDead();

    void setDead(boolean dead);

    void addInteractorConnection(Direction interactorConnection);

    void addNextNode(Direction direction, NetworkNode<T> nextNode, boolean sync);

    void onNextNodeAdded(NetworkNode<T> nextNode, Direction direction);

    NetworkNode<T> removeNextNode(Direction direction, boolean sync);

    NetworkNode<T> getNextNode(Direction direction);

    boolean hasInteractorConnection(Direction direction);

    boolean hasNextNodes();

    boolean hasInteractorConnections();

    boolean isUninitialized();

    TransportNetwork<T> getNetwork();

    BlockPos getPos();
}
