package com.thepigcat.transportlib.client;

import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.api.TransportNetwork;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNodes {
    public static final Map<TransportNetwork<?>, Map<BlockPos, NetworkNode<?>>> NODES = new ConcurrentHashMap<>();
    public static final Map<TransportNetwork<?>, Set<BlockPos>> INTERACTORS = new ConcurrentHashMap<>();
}