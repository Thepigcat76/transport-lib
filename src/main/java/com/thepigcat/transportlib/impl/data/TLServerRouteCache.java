package com.thepigcat.transportlib.impl.data;


import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.api.cache.RouteCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

// SERVER ONLY - Lazily loaded when route is first needed
public final class TLServerRouteCache {
    private static final Map<TransportNetworkImpl<?>, Map<ResourceKey<Level>, RouteCache<?>>> CACHE = new HashMap<>();

    public static void clearAll() {
        CACHE.clear();
    }

    public static void clear(ServerLevel level) {
        for (Map<ResourceKey<Level>, RouteCache<?>> caches : CACHE.values()) {
            caches.remove(level.dimension());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull RouteCache<T> getCache(TransportNetworkImpl<T> network, ServerLevel level) {
        return (RouteCache<T>) CACHE
                .computeIfAbsent(network, k -> new HashMap<>())
                .computeIfAbsent(level.dimension(), k -> new RouteCache<>());
    }
}
