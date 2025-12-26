package com.thepigcat.transportlib.client;

import com.thepigcat.transportlib.api.NetworkNodeImpl;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNodes {
    public static final Map<TransportNetworkImpl<?>, Map<BlockPos, NetworkNodeImpl<?>>> NODES = new ConcurrentHashMap<>();
    public static final Map<TransportNetworkImpl<?>, Set<BlockPos>> INTERACTORS = new ConcurrentHashMap<>();
}