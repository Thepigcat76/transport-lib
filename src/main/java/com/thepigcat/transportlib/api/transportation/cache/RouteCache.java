package com.thepigcat.transportlib.api.transportation.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.TransportLibUtils;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record RouteCache<T>(Map<BlockPos, List<NetworkRoute<T>>> routes) {
    public RouteCache() {
        this(new HashMap<>());
    }

    public static <T> Codec<RouteCache<T>> codec(TransportNetwork<T> network) {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.unboundedMap(Codec.STRING, NetworkRoute.codec(network).listOf()).fieldOf("routes").forGetter(cache -> TransportLibUtils.encodePosMap(cache.routes))
        ).apply(inst, routes -> new RouteCache<>(TransportLibUtils.decodePosMap(routes))));
    }

    public List<NetworkRoute<T>> computeIfAbsent(BlockPos key, Function<BlockPos, List<NetworkRoute<T>>> mappingFunction) {
        return this.routes.computeIfAbsent(key, mappingFunction);
    }

}