package com.thepigcat.transportlib.client.transportation;

import com.thepigcat.transportlib.api.transportation.NetworkNode;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNodes {
    public static final Map<TransportNetwork<?>, Map<BlockPos, NetworkNode<?>>> NODES = new ConcurrentHashMap<>();
    public static final Map<TransportNetwork<?>, Set<BlockPos>> INTERACTORS = new ConcurrentHashMap<>();
}