package com.thepigcat.transportlib.impl.data;


import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.api.cache.NetworkRoute;
import com.thepigcat.transportlib.api.cache.RouteCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SERVER ONLY
public final class TLServerRouteCache {
    // Possibly we might have to save the base data for calculating caches on server startup to reduce lag
    public static final Map<TransportNetworkImpl<?>, Map<ResourceKey<Level>, RouteCache<?>>> CACHE = new HashMap<>();

    public static <T> void add(TransportNetworkImpl<T> network, ServerLevel level, BlockPos originPos, NetworkRoute<T> route) {
        getCache(network, level)
                .routes().computeIfAbsent(originPos, k -> new ArrayList<>())
                .add(route);
    }

    public static <T> List<NetworkRoute<T>> getRoutes(TransportNetworkImpl<T> network, ServerLevel level, BlockPos originPos) {
        return getCache(network, level).computeIfAbsent(originPos, k -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull RouteCache<T> getCache(TransportNetworkImpl<T> network, ServerLevel level) {
        return (RouteCache<T>) CACHE
                .computeIfAbsent(network, k -> new HashMap<>())
                .computeIfAbsent(level.dimension(), k -> new RouteCache<>());
    }
}