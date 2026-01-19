package com.thepigcat.transportlib.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.TransportLibUtils;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.api.Transporting;
import com.thepigcat.transportlib.api.TransportingHandler;
import com.thepigcat.transportlib.api.cache.NetworkRoute;
import com.thepigcat.transportlib.networking.AddNextNodePayload;
import com.thepigcat.transportlib.networking.RemoveNextNodePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NetworkNodeImpl<T> implements NetworkNode<T> {
    public static final MapCodec<NetworkNodeImpl<?>> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TransportNetwork.CODEC.fieldOf("network").forGetter(NetworkNodeImpl::getNetwork),
            BlockPos.CODEC.fieldOf("pos").forGetter(NetworkNodeImpl::getPos),
            Codec.unboundedMap(StringRepresentable.fromEnum(Direction::values), BlockPos.CODEC).fieldOf("next").forGetter(NetworkNodeImpl::getNextAsPos),
            TransportLibUtils.TAG_CODEC.listOf().fieldOf("transporting").forGetter(NetworkNodeImpl::encodeTransporting),
            Codec.BOOL.fieldOf("dead").forGetter(NetworkNodeImpl::isDead),
            Direction.CODEC.listOf().fieldOf("interactor").forGetter(node -> List.copyOf(node.interactorConnections))
    ).apply(inst, NetworkNodeImpl::codecNew));

    private final TransportNetwork<T> network;
    private final BlockPos pos;
    private Map<Direction, NetworkNode<T>> next;
    private Map<Direction, BlockPos> uninitializedNext;
    private final List<Transporting<T>> transporting;
    private boolean dead;
    private final Set<Direction> interactorConnections;

    public NetworkNodeImpl(TransportNetwork<T> network, BlockPos pos) {
        this.network = network;
        this.pos = pos;
        this.next = new ConcurrentHashMap<>();
        this.transporting = new ArrayList<>();
        this.interactorConnections = new HashSet<>();
    }

    public NetworkNodeImpl(TransportNetwork<?> network, BlockPos pos, Map<Direction, BlockPos> next, List<Transporting<T>> transporting, boolean dead, Collection<Direction> interactorConnections) {
        this.network = (TransportNetwork<T>) network;
        this.pos = pos;
        this.uninitializedNext = next;
        this.transporting = transporting;
        this.dead = dead;
        this.interactorConnections = new HashSet<>(interactorConnections);
    }

    private static List<Tag> encodeTransporting(NetworkNodeImpl<?> node) {
        Codec<Object> codec = (Codec<Object>) node.network.getTransportingHandler().valueCodec();
        List<? extends Transporting<?>> transportings = node.getTransporting();
        return transportings.stream().map(t -> codec.encodeStart(NbtOps.INSTANCE, t.getValue()).getOrThrow()).toList();
    }

    private static <T> NetworkNodeImpl<T> codecNew(TransportNetwork<?> network, BlockPos pos, Map<Direction, BlockPos> next, List<Tag> transporting, boolean dead, Collection<Direction> interactorConnections) {
        List<Transporting<T>> list = transporting.stream().map(tag -> {
            TransportNetwork<T> transportNetwork = (TransportNetwork<T>) network;
            TransportingHandler<T> transportingHandler = transportNetwork.getTransportingHandler();
            Transporting<T> transporting1 = transportingHandler.createTransporting(transportNetwork);
            transporting1.setValue(transportingHandler.valueCodec().decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst());
            return transporting1;
        }).toList();
        return new NetworkNodeImpl<>(network, pos, next, list, dead, interactorConnections);
    }

    @Override
    public void initialize(Map<BlockPos, NetworkNode<T>> nodes) {
        this.next = new ConcurrentHashMap<>();
        for (Map.Entry<Direction, BlockPos> entry : this.uninitializedNext.entrySet()) {
            this.next.put(entry.getKey(), nodes.get(entry.getValue()));
        }
        this.uninitializedNext = null;
    }

    public TransportNetwork<T> getNetwork() {
        return network;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void removeNext(Direction direction) {
        this.next.remove(direction);
    }

    public void addNextNodeSynced(Direction direction, NetworkNode<T> nextNode) {
        this.next.put(direction, nextNode);
        if (this.network.isSynced()) {
            PacketDistributor.sendToAllPlayers(new AddNextNodePayload(this.network, this.pos, direction, nextNode.getPos()));
        }
    }

    public void removeNextNodeSynced(Direction direction) {
        this.next.remove(direction);
        if (this.network.isSynced()) {
            PacketDistributor.sendToAllPlayers(new RemoveNextNodePayload(this.network, this.pos, direction));
        }
    }

    @Override
    public void onConnect(ServerLevel serverLevel, BlockPos updatedPos, Direction updatedPosDirection) {
        NetworkNode<T> nextNode = this.network.findNextNode(null, serverLevel, this.pos, updatedPosDirection);
        if (nextNode != null && !nextNode.isDead()) {
            this.addNextNodeSynced(updatedPosDirection, nextNode);
        }
    }

    public void onDisconnect(ServerLevel serverLevel, BlockPos updatedPos, Direction updatedPosDirection) {
        NetworkNode<T> nextNode = this.network.findNextNode(null, serverLevel, this.pos, updatedPosDirection);
        if (nextNode != null) {
            this.removeNextNodeSynced(updatedPosDirection);
        }
    }

    @Override
    public void addNextNode(Direction direction, NetworkNode<T> node, boolean sync) {
        if (sync) {
            this.addNextNodeSynced(direction, node);
        } else {
            this.next.put(direction, node);
        }
    }

    @Override
    public void onNextNodeAdded(NetworkNode<T> nextNode, Direction direction) {
        this.addNextNodeSynced(direction, nextNode);
    }

    @Override
    public NetworkNode<T> removeNextNode(Direction direction, boolean sync) {
        NetworkNode<T> node = this.next.remove(direction);
        if (this.network.isSynced() && sync) {
            PacketDistributor.sendToAllPlayers(new RemoveNextNodePayload(this.network, this.pos, direction));
        }
        return node;
    }

    @Override
    public NetworkNode<T> getNextNode(Direction direction) {
        return this.next.get(direction);
    }

    @Override
    public boolean hasInteractorConnection(Direction direction) {
        return this.interactorConnections.contains(direction);
    }

    @Override
    public boolean hasNextNodes() {
        return !this.next.isEmpty();
    }

    @Override
    public boolean hasInteractorConnections() {
        return !this.interactorConnections.isEmpty();
    }

    @Override
    public boolean isUninitialized() {
        return uninitializedNext != null && next == null;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void addInteractorConnection(Direction interactorConnection) {
        if (interactorConnection != null) {
            this.interactorConnections.add(interactorConnection);
        }
    }

    public Map<Direction, NetworkNode<T>> getNext() {
        return next;
    }

    public int getConnectionsAmount() {
        return getNext().size() + getInteractorConnectionsAmount();
    }

    public int getInteractorConnectionsAmount() {
        return interactorConnections != null ? 1 : 0;
    }

    @Override
    public List<Transporting<T>> getTransporting() {
        return transporting;
    }

    public boolean isDead() {
        return dead;
    }

    public Set<Direction> getInteractorConnections() {
        return this.interactorConnections;
    }

    // FIXME: This does not work at all, it always returns an empty map
    private Map<Direction, BlockPos> getNextAsPos() {
        if (next != null) {
            Map<Direction, NetworkNode<T>> snapshot = new HashMap<>(next);
            return snapshot.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getPos()))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        }
        return Collections.emptyMap();
    }

    public void tick() {
        for (Transporting<T> transporting : this.getTransporting()) {
            NetworkRoute<T> route = transporting.getRoute();

        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkNode<?> that)) return false;
        return this.getPos().equals(that.getPos());
    }

    @Override
    public int hashCode() {
        return this.getPos().hashCode();
    }

}