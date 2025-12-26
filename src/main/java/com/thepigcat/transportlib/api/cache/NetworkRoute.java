package com.thepigcat.transportlib.api.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.api.NetworkNodeImpl;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

public class NetworkRoute<T> {
    private final BlockPos originPos;
    private final Set<NetworkNodeImpl<T>> path;
    private BlockPos interactorDest;
    private Direction interactorDirection;
    private int physicalDistance;
    private boolean valid;

    public NetworkRoute(BlockPos originPos, Set<NetworkNodeImpl<T>> path) {
        this.originPos = originPos;
        this.path = path;
    }

    public static <T> Codec<NetworkRoute<T>> codec(TransportNetworkImpl<T> network) {
        return RecordCodecBuilder.create(inst -> inst.group(
                BlockPos.CODEC.fieldOf("origin_pos").forGetter(NetworkRoute::getOriginPos),
                BlockPos.CODEC.fieldOf("interactor_dest").forGetter(NetworkRoute::getInteractorDest),
                Direction.CODEC.fieldOf("interactor_dir").forGetter(NetworkRoute::getInteractorDirection),
                Codec.INT.fieldOf("physical_distance").forGetter(NetworkRoute::getPhysicalDistance),
                NetworkNodeImpl.codec(network).listOf().fieldOf("path").forGetter(route -> route.path.stream().toList())
        ).apply(inst, NetworkRoute::codecNew));
    }

    public void setPhysicalDistance(int physicalDistance) {
        this.physicalDistance = physicalDistance;
    }

    public void setInteractorDest(BlockPos interactorDest) {
        this.interactorDest = interactorDest;
    }

    public void setInteractorDirection(Direction interactorDirection) {
        this.interactorDirection = interactorDirection;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Direction getInteractorDirection() {
        return interactorDirection;
    }

    public int getPhysicalDistance() {
        return physicalDistance;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public BlockPos getInteractorDest() {
        return interactorDest;
    }

    public Set<NetworkNodeImpl<T>> getPath() {
        return path;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NetworkRoute<?>) obj;
        return Objects.equals(this.originPos, that.originPos) &&
                Objects.equals(this.interactorDest, that.interactorDest) &&
                Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originPos, interactorDest, path);
    }

    @Override
    public String toString() {
        return "NetworkRoute[" +
                "originPos=" + originPos + ", " +
                "interactorDestinations=" + interactorDest + ", " +
                "path=" + path + ']';
    }

    private static <T> NetworkRoute<T> codecNew(BlockPos originPos, BlockPos interactorDest, Direction interactorDirection, int physicalDistance, List<NetworkNodeImpl<T>> path) {
        NetworkRoute<T> route = new NetworkRoute<>(originPos, new LinkedHashSet<>(path));
        route.interactorDest = interactorDest;
        route.physicalDistance = physicalDistance;
        route.interactorDirection = interactorDirection;
        return route;
    }

}