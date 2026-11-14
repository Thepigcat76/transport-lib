package com.thepigcat.transportlib.api.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Caches the routes of a single network in a single level.
 * <br>
 * Routes are cached per origin position and per set of directions the origin transports into,
 * so an origin that sends into changing directions does not constantly replace its own routes.
 * <br>
 * The cache does not validate its routes. It needs to be invalidated whenever the nodes, connections or
 * interactors of the network change, the routes are then lazily recomputed the next time they are needed.
 */
public final class RouteCache<T> {
    private final Map<BlockPos, Map<Set<Direction>, List<NetworkRoute<T>>>> routes = new HashMap<>();

    /**
     * @return the cached routes or {@code null} if no routes are cached. An empty list means that it is
     * known that there are no routes
     */
    public @Nullable List<NetworkRoute<T>> get(BlockPos originPos, Set<Direction> directions) {
        Map<Set<Direction>, List<NetworkRoute<T>>> originRoutes = this.routes.get(originPos);
        return originRoutes != null ? originRoutes.get(directions) : null;
    }

    public void put(BlockPos originPos, Set<Direction> directions, List<NetworkRoute<T>> routes) {
        this.routes.computeIfAbsent(originPos.immutable(), k -> new HashMap<>(2))
                .put(Set.copyOf(directions), List.copyOf(routes));
    }

    public void invalidate() {
        this.routes.clear();
    }

    public void invalidate(BlockPos originPos) {
        this.routes.remove(originPos);
    }

    public boolean isEmpty() {
        return this.routes.isEmpty();
    }

}
