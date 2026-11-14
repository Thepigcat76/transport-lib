package com.thepigcat.transportlib.impl;

import com.mojang.serialization.MapCodec;
import com.thepigcat.transportlib.api.*;
import com.thepigcat.transportlib.api.cache.NetworkRoute;
import com.thepigcat.transportlib.api.cache.RouteCache;
import com.thepigcat.transportlib.impl.data.TLServerRouteCache;
import com.thepigcat.transportlib.impl.data.NodeNetworkData;
import com.thepigcat.transportlib.impl.data.NodeNetworkSavedData;
import com.thepigcat.transportlib.networking.AddInteractorPayload;
import com.thepigcat.transportlib.networking.AddNetworkNodePayload;
import com.thepigcat.transportlib.networking.RemoveInteractorPayload;
import com.thepigcat.transportlib.networking.RemoveNetworkNodePayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class TransportNetworkImpl<T> implements TransportNetwork<T> {
    private final BiFunction<TransportNetworkImpl<T>, BlockPos, NetworkNode<T>> nodeFactory;
    private final MapCodec<? extends NetworkNode<?>> nodeCodec;
    private final TransportingHandler<T> transportingHandler;
    private final BiFunction<ServerLevel, NetworkNode<T>, Float> lossPerBlockFunction;
    private final Supplier<TransferSpeed> transferSpeedFunction;
    private final InteractorCheckFunction interactorCheckFunction;
    private final int maxConnectionDistance;
    private final StreamCodec<ByteBuf, T> streamCodec;

    protected TransportNetworkImpl(Builder<T> builder) {
        this.nodeFactory = builder.nodeFactory;
        this.nodeCodec = builder.nodeCodec;
        this.transportingHandler = builder.transportingHandler;
        this.lossPerBlockFunction = builder.lossPerBlockFunction;
        this.transferSpeedFunction = builder.transferSpeedFunction;
        this.interactorCheckFunction = builder.interactorCheckFunction;
        this.maxConnectionDistance = builder.maxConnectionDistance;
        this.streamCodec = builder.streamCodec;
    }

    @Override
    public NetworkNode<T> createNode(BlockPos pos) {
        return this.nodeFactory.apply(this, pos);
    }

    @Override
    public MapCodec<? extends NetworkNode<?>> getNodeCodec() {
        return nodeCodec;
    }

    @Override
    public Transporting<T> createTransporting() {
        return new TransportingImpl<>(this);
    }

    @Override
    public void connect(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1) {
        NetworkNode<T> node0 = this.findNextNode(null, serverLevel, pos, direction0);
        if (node0 != null && !node0.isDead()) {
            node0.onConnect(serverLevel, pos, direction0.getOpposite());
        }
        NetworkNode<T> node1 = this.findNextNode(null, serverLevel, pos, direction1);
        if (node1 != null && !node1.isDead()) {
            node1.onConnect(serverLevel, pos, direction1.getOpposite());
        }
        // New connections can create new (shorter) routes
        this.invalidateRoutes(serverLevel);
    }

    @Override
    public void disconnect(ServerLevel serverLevel, BlockPos pos, Direction direction0, Direction direction1) {
        this.removeConnectionInDir(serverLevel, pos, direction0);
        this.removeConnectionInDir(serverLevel, pos, direction1);
        this.invalidateRoutes(serverLevel);
    }

    @Override
    public void addInteractor(ServerLevel serverLevel, BlockPos interactorPos) {
        if (!this.getInteractors(serverLevel).add(interactorPos.immutable())) {
            return;
        }
        getNetworkData(serverLevel).setDirty();
        if (this.isSynced()) {
            PacketDistributor.sendToAllPlayers(new AddInteractorPayload(this, interactorPos));
        }
    }

    @Override
    public void removeInteractor(ServerLevel serverLevel, BlockPos interactorPos) {
        boolean changed = this.getInteractors(serverLevel).remove(interactorPos);
        if (changed && this.isSynced()) {
            PacketDistributor.sendToAllPlayers(new RemoveInteractorPayload(this, interactorPos));
        }

        // Adjacent nodes would otherwise keep their interactor connection, and recomputed routes would still lead to the removed interactor.
        // The interactor can also still be there if only one of the nodes connected to it got removed, so we only remove the
        // connection if the node can no longer interact with it
        for (Direction direction : Direction.values()) {
            NetworkNode<T> node = this.getNodeAt(serverLevel, interactorPos.relative(direction));
            Direction nodeDirection = direction.getOpposite();
            if (node != null && node.hasInteractorConnection(nodeDirection) && !this.checkForInteractorAt(serverLevel, node.getPos(), nodeDirection)) {
                node.removeInteractorConnection(nodeDirection);
                this.syncNode(node);
                changed = true;
            }
        }

        // This is also called for positions that never were interactors, nothing to do in that case
        if (changed) {
            getNetworkData(serverLevel).setDirty();
            this.invalidateRoutes(serverLevel);
        }
    }

    /**
     * @return whether any node of this network is connected to the interactor at {@code interactorPos}
     */
    protected boolean isInteractorConnected(ServerLevel serverLevel, BlockPos interactorPos) {
        for (Direction direction : Direction.values()) {
            NetworkNode<T> node = this.getNodeAt(serverLevel, interactorPos.relative(direction));
            if (node != null && node.hasInteractorConnection(direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addNode(NetworkNode<T> node, ServerLevel level, BlockPos pos, Direction[] connections, boolean dead) {
        // Avoid replacing an identical node, this happens e.g. whenever a connected interactor changes its block state
        NetworkNode<T> existingNode = this.getNodeAt(level, pos);
        if (existingNode != null && this.isNodeUpToDate(existingNode, level, pos, connections, dead)) {
            return;
        }

        // The old node is replaced, its neighbors must not keep pointing to it in directions the new node no longer connects to
        if (existingNode != null) {
            this.unlinkNode(existingNode);
        }

        node.setDead(dead);

        for (Direction direction : Direction.values()) {
            if (this.checkForInteractorAt(level, pos, direction)) {
                node.addInteractorConnection(direction);
                this.addInteractor(level, pos.relative(direction));
            }
        }

        this.addNodeSynced(level, pos, node);

        if (!dead) {
            for (Direction connection : connections) {
                if (connection != null) {
                    NetworkNode<T> nextNode = this.findNextNode(node, level, pos, connection);
                    if (nextNode != null && !nextNode.isDead()) {
                        node.addNextNode(connection, nextNode, true);
                        nextNode.onNextNodeAdded(node, connection.getOpposite());
                    }
                }
            }
        }

        // New nodes can create new (shorter) routes or connect new interactors
        this.invalidateRoutes(level);
    }

    /**
     * @return whether adding a node with these parameters would not change the existing node
     */
    protected boolean isNodeUpToDate(NetworkNode<T> existingNode, ServerLevel level, BlockPos pos, Direction[] connections, boolean dead) {
        if (existingNode.isDead() != dead) return false;

        for (Direction direction : Direction.values()) {
            boolean interactor = this.checkForInteractorAt(level, pos, direction);
            if (interactor != existingNode.hasInteractorConnection(direction)) {
                return false;
            }
            if (interactor && !this.hasInteractorAt(level, pos.relative(direction))) {
                return false;
            }
        }

        Set<Direction> connectionSet = EnumSet.noneOf(Direction.class);
        if (!dead) {
            for (Direction connection : connections) {
                if (connection != null) {
                    connectionSet.add(connection);
                    NetworkNode<T> nextNode = this.findNextNode(existingNode, level, pos, connection);
                    NetworkNode<T> expectedNext = nextNode != null && !nextNode.isDead() ? nextNode : null;
                    if (!Objects.equals(expectedNext, existingNode.getNextNode(connection))) {
                        return false;
                    }
                }
            }
        }

        for (Direction direction : Direction.values()) {
            if (!connectionSet.contains(direction) && existingNode.getNextNode(direction) != null) {
                return false;
            }
        }

        return true;
    }

    private void addNodeSynced(ServerLevel level, BlockPos pos, NetworkNode<T> node) {
        NodeNetworkSavedData networks = getNetworkData(level);
        getServerNodes(level).put(pos.immutable(), node);
        this.syncNode(node);
        networks.setDirty();
    }

    private void syncNode(NetworkNode<T> node) {
        if (this.isSynced()) {
            Tag result = NetworkNode.CODEC.encodeStart(NbtOps.INSTANCE, node).getOrThrow();
            PacketDistributor.sendToAllPlayers(new AddNetworkNodePayload(this, node.getPos(), result));
        }
    }

    /**
     * Removes the connections of all neighboring nodes to this node
     */
    private void unlinkNode(NetworkNode<T> node) {
        for (Direction direction : Direction.values()) {
            NetworkNode<T> nextNode = node.getNextNode(direction);
            if (nextNode != null && Objects.equals(nextNode.getNextNode(direction.getOpposite()), node)) {
                nextNode.removeNextNode(direction.getOpposite(), true);
            }
        }
    }

    @Override
    public @Nullable NetworkNode<T> removeNode(ServerLevel serverLevel, BlockPos pos) {
        NetworkNode<T> node = this.removeNodeSynced(serverLevel, pos);
        if (node == null) {
            return null;
        }

        this.unlinkNode(node);

        if (node.hasInteractorConnections()) {
            for (Direction connection : Direction.values()) {
                if (node.hasInteractorConnection(connection)) {
                    BlockPos relative = pos.relative(connection);
                    // Other nodes can still be connected to the interactor, it only stops being one when the last node is gone
                    if (!this.isInteractorConnected(serverLevel, relative)) {
                        this.removeInteractor(serverLevel, relative);
                    }
                }
            }
        }
        this.setServerNodesChanged(serverLevel);
        this.invalidateRoutes(serverLevel);

        return node;
    }

    private @Nullable NetworkNode<T> removeNodeSynced(ServerLevel serverLevel, BlockPos pos) {
        NodeNetworkSavedData networks = getNetworkData(serverLevel);
        NetworkNode<T> removedNode = this.getServerNodes(serverLevel).remove(pos);
        networks.setDirty();
        if (this.isSynced()) {
            PacketDistributor.sendToAllPlayers(new RemoveNetworkNodePayload(this, pos));
        }
        return removedNode;
    }

    @Override
    public @Nullable NetworkNode<T> getNodeAt(ServerLevel serverLevel, BlockPos pos) {
        return getServerNodes(serverLevel).get(pos);
    }

    @Override
    public boolean hasNodeAt(ServerLevel serverLevel, BlockPos pos) {
        return getServerNodes(serverLevel).containsKey(pos);
    }

    @Override
    public boolean checkForInteractorAt(ServerLevel serverLevel, BlockPos nodePos, Direction direction) {
        return this.interactorCheckFunction.test(serverLevel, nodePos, nodePos.relative(direction), direction);
    }

    @Override
    public boolean hasInteractorAt(ServerLevel serverLevel, BlockPos interactorPos) {
        NodeNetworkSavedData networks = getNetworkData(serverLevel);
        return networks.getData().getOrDefault(this, NodeNetworkData.empty()).interactors().contains(interactorPos);
    }

    @Override
    public @Nullable NetworkNode<T> findNextNode(@Nullable NetworkNode<T> selfNode, ServerLevel serverLevel, BlockPos pos, Direction direction, Set<BlockPos> ignoredNodes) {
        // Pathfinding should not walk through interactors
        if (this.hasInteractorAt(serverLevel, pos.relative(direction))) {
            return null;
        }

        Map<BlockPos, NetworkNode<T>> nodes = this.getServerNodes(serverLevel);

        // With a limited connection distance it is cheaper to walk along the direction than to look at every node of the network
        if (this.maxConnectionDistance >= 0 && this.maxConnectionDistance < nodes.size()) {
            BlockPos.MutableBlockPos mutablePos = pos.mutable();
            for (int i = 0; i < this.maxConnectionDistance; i++) {
                mutablePos.move(direction);
                NetworkNode<T> node = nodes.get(mutablePos);
                if (node != null && node != selfNode && !ignoredNodes.contains(mutablePos)) {
                    return node;
                }
            }
            return null;
        }

        NetworkNode<T> nearestNode = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Map.Entry<BlockPos, NetworkNode<T>> entry : nodes.entrySet()) {
            BlockPos pos1 = entry.getKey();
            if (entry.getValue() != selfNode && areNodesAligned(pos, pos1, direction)) {
                int distance = getDirectionalDistance(pos, pos1, direction);
                if (distance < nearestDistance && !ignoredNodes.contains(pos1)) {
                    nearestNode = entry.getValue();
                    nearestDistance = distance;
                }
            }
        }

        if (this.maxConnectionDistance >= 0 && nearestDistance > this.maxConnectionDistance) {
            return null;
        }

        return nearestNode;
    }

    /**
     * @param value      The value to be transported
     * @param directions the directions the value should be transported to (only relevant for initial node). If the array is empty, the value will be transported in all directions
     * @return the remaining value that was not transported anywhere, returns {@link TransportingHandler#defaultValue()} if everything was distributed
     */
    @Override
    public T transport(ServerLevel serverLevel, BlockPos pos, T value, boolean simulate, Direction... directions) {
        if (this.getTransportingHandler().validTransportValue(value) && this.hasInteractorAt(serverLevel, pos)) {
            Direction[] directions1;
            if (directions.length == 0) {
                directions1 = Direction.values();
            } else {
                directions1 = directions;
            }
            List<NetworkNode<T>> nodes = getNodesAroundPos(serverLevel, pos, directions1);

            // Routes are cached per position, senders that transport into different directions need different routes
            Set<Direction> directionSet = EnumSet.noneOf(Direction.class);
            for (Direction direction : directions1) {
                if (direction != null) {
                    directionSet.add(direction);
                }
            }
            RouteCache<T> routeCache = this.getRouteCache(serverLevel);
            List<NetworkRoute<T>> routes = routeCache.get(pos, directionSet);
            if (routes == null) {
                routes = this.computeRoutesForCache(pos, nodes);
                routeCache.put(pos, directionSet, routes);
            }
            // If transportation is instant, we perform network traversal in the function
            // and return the remainder directly, otherwise there will be no remainder and
            // the values will be stored in the network
            if (this.transferSpeedFunction.get().isInstant()) {
                T remainder = this.getTransportingHandler().defaultValue();
                if (!routes.isEmpty()) {
                    List<T> split1 = this.transportingHandler.split(value, routes.size());
                    for (int i = 0; i < routes.size(); i++) {
                        NetworkRoute<T> route = routes.get(i);
                        remainder = this.getTransportingHandler().join(remainder, this.getTransportingHandler().receive(serverLevel, route.getInteractorDest(), simulate, route.getInteractorDirection(), split1.get(i)));
                    }
                } else {
                    remainder = value;
                }

                return remainder;
            } else {
                if (nodes.isEmpty()) {
                    return value;
                }
                List<T> split = this.transportingHandler.split(value, nodes.size());

                for (int i = 0; i < nodes.size(); i++) {
                    NetworkNode<T> node = nodes.get(i);
                    Transporting<T> transporting = new TransportingImpl<>(this);
                    T splitValue = split.get(i);
                    transporting.setValue(splitValue);
                    transporting.trySyncValue(node.getPos());
                }
            }

            return getTransportingHandler().defaultValue();
        }
        return value;
    }

    protected @NotNull List<NetworkNode<T>> getNodesAroundPos(ServerLevel serverLevel, BlockPos pos, Direction... directions1) {
        List<NetworkNode<T>> nodes = new ArrayList<>();
        for (Direction direction : directions1) {
            if (direction == null) continue;
            BlockPos relative = pos.relative(direction);
            if (this.hasNodeAt(serverLevel, relative)) {
                nodes.add(this.getNodeAt(serverLevel, relative));
            }
        }
        return nodes;
    }

    public Set<BlockPos> getInteractors(ServerLevel serverLevel) {
        return getRawNetworkData(this, serverLevel).interactors();
    }

    private void removeConnectionInDir(ServerLevel serverLevel, BlockPos pos, Direction direction) {
        NetworkNode<T> node1 = this.findNextNode(null, serverLevel, pos, direction);
        if (node1 != null) {
            node1.onDisconnect(serverLevel, pos, direction.getOpposite());
        }
    }

    /**
     * Invalidates all cached routes of this network in the level. This needs to be called whenever the nodes,
     * their connections or the interactors of the network change. The routes are recomputed when they are needed again
     */
    public void invalidateRoutes(ServerLevel serverLevel) {
        this.getRouteCache(serverLevel).invalidate();
    }

    public Map<BlockPos, NetworkNode<T>> getServerNodes(ServerLevel level) {
        NodeNetworkData<T> map = getRawNetworkData(this, level);
        return map.nodes();
    }

    public static <T> NodeNetworkData<T> getRawNetworkData(TransportNetwork<T> network, ServerLevel level) {
        NodeNetworkSavedData networkSavedData = getNetworkData(level);
        Map<TransportNetwork<?>, NodeNetworkData<?>> networks = networkSavedData.getData();
        NodeNetworkData<T> map;
        if (!networks.containsKey(network)) {
            map = (NodeNetworkData<T>) networks.computeIfAbsent(network, k -> NodeNetworkData.empty());
            networkSavedData.setDirty();
        } else {
            map = (NodeNetworkData<T>) networks.get(network);
        }
        return map;
    }

    public RouteCache<T> getRouteCache(ServerLevel serverLevel) {
        return TLServerRouteCache.getCache(this, serverLevel);
    }

    public void setServerNodesChanged(ServerLevel serverLevel) {
        getNetworkData(serverLevel).setDirty();
    }

    // ALGORITHM FOR FINDING THE SHORTEST ROUTES
    // We run Dijkstra's algorithm over the node graph, starting at all nodes next to the origin position at once.
    // Nodes are visited in order of their physical distance to the origin, so the first time we reach an
    // interactor we have found the shortest route to it. Every node is only visited once, which makes this
    // O((nodes + connections) * log(nodes)) instead of walking every possible path through the network.
    //
    // These caches are lazy loaded when we first want to send something through the net.

    /**
     * Computes the shortest route from {@code pos} to every interactor that can be reached through {@code nodes}
     *
     * @param pos   the origin position (the interactor that sends the value)
     * @param nodes the nodes next to the origin position the routes can start at
     * @return one route per reachable interactor, sorted by physical distance (shortest first)
     */
    protected @NotNull List<NetworkRoute<T>> computeRoutesForCache(BlockPos pos, List<NetworkNode<T>> nodes) {
        BlockPos originPos = pos.immutable();
        List<NetworkRoute<T>> routes = new ArrayList<>();

        Map<BlockPos, Integer> distances = new HashMap<>();
        // The node we came from on the shortest path to the node at the position
        Map<BlockPos, NetworkNode<T>> previousNodes = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> foundInteractors = new HashSet<>();
        PriorityQueue<PathEntry<T>> queue = new PriorityQueue<>(Comparator.comparingInt(PathEntry::distance));

        for (NetworkNode<T> node : nodes) {
            if (distances.putIfAbsent(node.getPos(), 0) == null) {
                queue.add(new PathEntry<>(node, 0));
            }
        }

        while (!queue.isEmpty()) {
            PathEntry<T> entry = queue.poll();
            NetworkNode<T> node = entry.node();
            BlockPos nodePos = node.getPos();
            // Outdated entries of nodes we already reached on a shorter path
            if (!visited.add(nodePos)) continue;

            if (node.hasInteractorConnections()) {
                for (Direction connection : Direction.values()) {
                    if (node.hasInteractorConnection(connection)) {
                        BlockPos interactorPos = nodePos.relative(connection);
                        if (!interactorPos.equals(originPos) && foundInteractors.add(interactorPos)) {
                            NetworkRoute<T> route = new NetworkRoute<>(originPos, this.buildPath(node, previousNodes));
                            route.setPhysicalDistance(entry.distance());
                            route.setInteractorDest(interactorPos);
                            route.setInteractorDirection(connection);
                            routes.add(route);
                        }
                    }
                }
            }

            for (Direction direction : Direction.values()) {
                NetworkNode<T> nextNode = node.getNextNode(direction);
                if (nextNode == null) continue;

                BlockPos nextPos = nextNode.getPos();
                if (visited.contains(nextPos) || nextPos.equals(originPos)) continue;

                int distance = entry.distance() + nodePos.distManhattan(nextPos);
                Integer knownDistance = distances.get(nextPos);
                if (knownDistance == null || distance < knownDistance) {
                    distances.put(nextPos, distance);
                    previousNodes.put(nextPos, node);
                    queue.add(new PathEntry<>(nextNode, distance));
                }
            }
        }

        return routes;
    }

    /**
     * @return the nodes on the path to {@code destination}, ordered from the origin to the destination
     */
    private Set<NetworkNode<T>> buildPath(NetworkNode<T> destination, Map<BlockPos, NetworkNode<T>> previousNodes) {
        List<NetworkNode<T>> reversedPath = new ArrayList<>();
        for (NetworkNode<T> node = destination; node != null; node = previousNodes.get(node.getPos())) {
            reversedPath.add(node);
        }
        Set<NetworkNode<T>> path = new LinkedHashSet<>(reversedPath.size() * 2);
        for (int i = reversedPath.size() - 1; i >= 0; i--) {
            path.add(reversedPath.get(i));
        }
        return path;
    }

    private record PathEntry<T>(NetworkNode<T> node, int distance) {
    }

    private static @NotNull NodeNetworkSavedData getNetworkData(ServerLevel serverLevel) {
        return NodeNetworkSavedData.getNetworkData(serverLevel);
    }

    private static boolean areNodesAligned(BlockPos pos0, BlockPos pos1, Direction direction) {
        int deltaX = Integer.signum(pos1.getX() - pos0.getX());
        int deltaY = Integer.signum(pos1.getY() - pos0.getY());
        int deltaZ = Integer.signum(pos1.getZ() - pos0.getZ());

        return deltaX == direction.getStepX() && deltaY == direction.getStepY() && deltaZ == direction.getStepZ();
    }

    private static int getDirectionalDistance(BlockPos origin, BlockPos target, Direction direction) {
        int diff;
        switch (direction.getAxis()) {
            case X -> diff = target.getX() - origin.getX();
            case Y -> diff = target.getY() - origin.getY();
            case Z -> diff = target.getZ() - origin.getZ();
            default -> throw new IllegalArgumentException("Invalid axis");
        }

        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            diff = -diff;
        }

        return diff;
    }

    public int getMaxConnectionDistance() {
        return maxConnectionDistance;
    }

    @Override
    public boolean isSynced() {
        return streamCodec != null;
    }

    @Override
    public TransportingHandler<T> getTransportingHandler() {
        return transportingHandler;
    }

    public static <T> Builder<T> builder(TransportingHandler<T> transportingHandler) {
        return new Builder<>(transportingHandler);
    }

    public static final class Builder<T> {
        private BiFunction<TransportNetworkImpl<T>, BlockPos, NetworkNode<T>> nodeFactory = NetworkNodeImpl::new;
        private MapCodec<? extends NetworkNode<?>> nodeCodec = NetworkNodeImpl.CODEC;
        private final TransportingHandler<T> transportingHandler;
        private BiFunction<ServerLevel, NetworkNode<T>, Float> lossPerBlockFunction = (l, n) -> 0F;
        private Supplier<TransferSpeed> transferSpeedFunction = () -> TransferSpeed.speed(1);
        private InteractorCheckFunction interactorCheckFunction = (l, p0, p1, d) -> false;
        private int maxConnectionDistance = -1;
        private StreamCodec<ByteBuf, T> streamCodec = null;

        private Builder(TransportingHandler<T> transportingHandler) {
            this.transportingHandler = transportingHandler;
        }

        public Builder<T> node(BiFunction<TransportNetworkImpl<T>, BlockPos, NetworkNode<T>> nodeFactory, MapCodec<? extends NetworkNode<?>> nodeCodec) {
            this.nodeFactory = nodeFactory;
            this.nodeCodec = nodeCodec;
            return this;
        }

        public Builder<T> lossPerBlock(BiFunction<ServerLevel, NetworkNode<T>, Float> lossPerBlockFunction) {
            this.lossPerBlockFunction = lossPerBlockFunction;
            return this;
        }

        public Builder<T> transferSpeed(Supplier<TransferSpeed> transferSpeedFunction) {
            this.transferSpeedFunction = transferSpeedFunction;
            return this;
        }

        @Deprecated
        public Builder<T> interactorCheck(TriPredicate<Level, BlockPos, Direction> interactorCheckFunction) {
            this.interactorCheckFunction = (level, cablePos, interactorPos, direction) -> interactorCheckFunction.test(level, cablePos, direction);
            return this;
        }

        public Builder<T> interactorCheck(InteractorCheckFunction function) {
            this.interactorCheckFunction = function;
            return this;
        }

        public Builder<T> maxConnectionDistance(int maxConnectionDistance) {
            this.maxConnectionDistance = maxConnectionDistance;
            return this;
        }

        public Builder<T> synced(StreamCodec<ByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public <N extends TransportNetworkImpl<T>> N build(Function<Builder<T>, N> constructor) {
            return constructor.apply(this);
        }

        public TransportNetworkImpl<T> build() {
            return this.build(TransportNetworkImpl::new);
        }
    }
}