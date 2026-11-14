package com.thepigcat.transportlib.api.cache;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thepigcat.transportlib.api.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

public class NetworkRoute<T> {
    private final BlockPos originPos;
    private final Set<NetworkNode<T>> path;
    private BlockPos interactorDest;
    private Direction interactorDirection;
    private int physicalDistance;

    private NetworkRoute(BlockPos originPos, Set<NetworkNode<T>> path, BlockPos interactorDest, Direction interactorDirection, int physicalDistance) {
        this.originPos = originPos;
        this.path = path;
        this.interactorDest = interactorDest;
        this.interactorDirection = interactorDirection;
        this.physicalDistance = physicalDistance;
    }

    public NetworkRoute(BlockPos originPos, Set<NetworkNode<T>> path) {
        this.originPos = originPos;
        this.path = path;
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

    public Set<NetworkNode<T>> getPath() {
        return path;
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

    private static <T> NetworkRoute<T> codecNew(BlockPos originPos, BlockPos interactorDest, Direction interactorDirection, int physicalDistance, List<NetworkNode<?>> path) {
        NetworkRoute<T> route = new NetworkRoute<>(originPos, (Set) new LinkedHashSet<>(path));
        route.interactorDest = interactorDest;
        route.physicalDistance = physicalDistance;
        route.interactorDirection = interactorDirection;
        return route;
    }

    public NetworkRoute<T> copy() {
        return new NetworkRoute<>(this.originPos, this.path, this.interactorDest, this.interactorDirection, this.physicalDistance);
    }
}