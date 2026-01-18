package com.thepigcat.transportlib.impl.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.TransportLibUtils;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.api.TransportNetwork;
import net.minecraft.core.BlockPos;

import java.util.*;

/** stores data for a single type of network
 * @param interactors TODO: Cache directions of interactor connections
 */
public record NodeNetworkData<T>(Map<BlockPos, NetworkNode<T>> nodes, Set<BlockPos> interactors) {
    public static <T> Codec<NodeNetworkData<T>> codec(TransportNetwork<T> network) {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.unboundedMap(Codec.STRING, NetworkNode.CODEC).fieldOf("nodes").forGetter(data -> (Map) TransportLibUtils.encodePosMap(data.nodes)),
                BlockPos.CODEC.listOf().fieldOf("interactors").forGetter(data -> List.copyOf(data.interactors))
        ).apply(inst, NodeNetworkData::codecNew));
    }

    private static <T> NodeNetworkData<T> codecNew(Map<String, NetworkNode<?>> stringNetworkNodeMap, List<BlockPos> interactors) {
        return new NodeNetworkData<>((Map) TransportLibUtils.decodePosMap(stringNetworkNodeMap), new HashSet<>(interactors));
    }

    public static <T> NodeNetworkData<T> empty() {
        return new NodeNetworkData<>(new HashMap<>(), new HashSet<>());
    }

}