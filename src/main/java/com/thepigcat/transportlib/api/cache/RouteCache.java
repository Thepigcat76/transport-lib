package com.thepigcat.transportlib.api.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.TransportLibUtils;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record RouteCache<T>(Map<BlockPos, List<NetworkRoute<T>>> routes) {
    public static final Codec<RouteCache<?>> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, NetworkRoute.CODEC.listOf()).fieldOf("routes").forGetter(cache -> (Map) TransportLibUtils.encodePosMap(cache.routes))
    ).apply(inst, routes -> new RouteCache<>((Map) TransportLibUtils.decodePosMap(routes))));

    public RouteCache() {
        this(new HashMap<>());
    }

    public List<NetworkRoute<T>> computeIfAbsent(BlockPos key, Function<BlockPos, List<NetworkRoute<T>>> mappingFunction) {
        return this.routes.computeIfAbsent(key, mappingFunction);
    }

}