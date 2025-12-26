package com.thepigcat.transportlib.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.api.cache.NetworkRoute;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.impl.TransportingImpl;
import com.thepigcat.transportlib.networking.AddNextNodePayload;
import com.thepigcat.transportlib.networking.RemoveNextNodePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NetworkNodeImpl<T> {
    private final TransportNetwork<T> network;
    private final BlockPos pos;
    private Map<Direction, NetworkNodeImpl<T>> next;
    private Map<Direction, BlockPos> uninitializedNext;
    private final TransportingImpl<T> transporting;
    private boolean dead;
    private Set<Direction> interactorConnection;

    public NetworkNodeImpl(TransportNetwork<T> network, BlockPos pos) {
        this.network = network;
        this.pos = pos;
        this.next = new ConcurrentHashMap<>();
        this.transporting = new TransportingImpl<>(network);
    }

    public NetworkNodeImpl(TransportNetwork<?> network, BlockPos pos, Map<Direction, BlockPos> next, TransportingImpl<T> transporting, boolean dead, Collection<Direction> interactorConnections) {
        this.network = (TransportNetwork<T>) network;
        this.pos = pos;
        this.uninitializedNext = next;
        this.transporting = transporting;
        this.dead = dead;
        this.interactorConnection = new HashSet<>(interactorConnections);
    }

    public void initialize(Map<BlockPos, ? extends NetworkNodeImpl<?>> nodes) {
        this.next = new ConcurrentHashMap<>();
        for (Map.Entry<Direction, BlockPos> entry : this.uninitializedNext.entrySet()) {
            this.next.put(entry.getKey(), (NetworkNodeImpl<T>) nodes.get(entry.getValue()));
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

    public void addNextNodeSynced(Direction direction, NetworkNodeImpl<T> nextNode) {
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

    public void onNextNodeAdded(NetworkNodeImpl<T> originNode, Direction originNodeDirection) {
        this.addNextNodeSynced(originNodeDirection, originNode);
    }

    public void onConnectionAdded(ServerLevel serverLevel, BlockPos updatedPos, Direction updatedPosDirection) {
        NetworkNodeImpl<T> nextNode = this.network.findNextNode(null, serverLevel, this.pos, updatedPosDirection);
        if (nextNode != null && !nextNode.isDead()) {
            this.addNextNodeSynced(updatedPosDirection, nextNode);
        }
    }

    public void onConnectionRemoved(ServerLevel serverLevel, BlockPos updatedPos, Direction updatedPosDirection) {
        NetworkNodeImpl<T> nextNode = this.network.findNextNode(null, serverLevel, this.pos, updatedPosDirection);
        if (nextNode != null) {
            this.removeNextNodeSynced(updatedPosDirection);
        }
    }

    public void addNext(Direction direction, NetworkNodeImpl<?> node) {
        this.next.put(direction, (NetworkNodeImpl<T>) node);
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void addInteractorConnection(Direction interactorConnection) {
        this.interactorConnection.add(interactorConnection);
    }

    public Map<Direction, NetworkNodeImpl<T>> getNext() {
        return next;
    }

    public int getConnectionsAmount() {
        return getNext().size() + getInteractorConnectionsAmount();
    }

    public int getInteractorConnectionsAmount() {
        return interactorConnection != null ? 1 : 0;
    }

    public TransportingImpl<T> getTransporting() {
        return transporting;
    }

    public boolean uninitialized() {
        return uninitializedNext != null && next == null;
    }

    public boolean isDead() {
        return dead;
    }

    public Set<Direction> getInteractorConnections() {
        return this.interactorConnection;
    }

    public List<NetworkRoute<T>> getCachesReferencingThis(ServerLevel serverLevel) {
        List<NetworkRoute<T>> affectedRoutes = new ArrayList<>();
        Map<BlockPos, List<NetworkRoute<T>>> routes = this.network.getRouteCache(serverLevel).routes();
        for (List<NetworkRoute<T>> value : routes.values()) {
            for (NetworkRoute<T> route : value) {
                if (route.getPath().contains(this)) {
                    affectedRoutes.add(route);
                }
            }
        }
        return affectedRoutes;
    }

    // FIXME: This does not work at all, it always returns an empty map
    private Map<Direction, BlockPos> getNextAsPos() {
        if (next != null) {
            Map<Direction, NetworkNodeImpl<T>> snapshot = new HashMap<>(next);
            return snapshot.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getPos()))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkNodeImpl<?> that)) return false;
        return dead == that.dead && Objects.equals(network, that.network) && Objects.equals(pos, that.pos) && Objects.equals(next, that.next) && Objects.equals(uninitializedNext, that.uninitializedNext) && Objects.equals(transporting, that.transporting) && interactorConnection == that.interactorConnection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, pos, transporting, dead, interactorConnection);
    }

    public static <T> Codec<NetworkNodeImpl<T>> codec(TransportNetworkImpl<T> network) {
        return RecordCodecBuilder.create(inst -> inst.group(
                TransportNetwork.CODEC.fieldOf("network").forGetter(NetworkNodeImpl::getNetwork),
                BlockPos.CODEC.fieldOf("pos").forGetter(NetworkNodeImpl::getPos),
                Codec.unboundedMap(StringRepresentable.fromEnum(Direction::values), BlockPos.CODEC).fieldOf("next").forGetter(NetworkNodeImpl::getNextAsPos),
                TransportingImpl.codec(network.codec()).fieldOf("transporting").forGetter(NetworkNodeImpl::getTransporting),
                Codec.BOOL.fieldOf("dead").forGetter(NetworkNodeImpl::isDead),
                Direction.CODEC.listOf().fieldOf("interactor").forGetter(node -> List.copyOf(node.interactorConnection))
        ).apply(inst, NetworkNodeImpl::new));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, NetworkNodeImpl<T>> streamCodec(TransportNetworkImpl<T> network) {
        return StreamCodec.composite(
                TransportNetworkImpl.STREAM_CODEC,
                NetworkNodeImpl::getNetwork,
                BlockPos.STREAM_CODEC,
                NetworkNodeImpl::getPos,
                ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, BlockPos.STREAM_CODEC),
                NetworkNodeImpl::getNextAsPos,
                TransportingImpl.streamCodec(network.streamCodec()),
                NetworkNodeImpl::getTransporting,
                ByteBufCodecs.BOOL,
                NetworkNodeImpl::isDead,
                ByteBufCodecs.collection(HashSet::new, Direction.STREAM_CODEC),
                node -> node.interactorConnection,
                NetworkNodeImpl::new
        );
    }

}