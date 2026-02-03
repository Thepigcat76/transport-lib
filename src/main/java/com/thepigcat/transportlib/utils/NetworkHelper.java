package com.thepigcat.transportlib.utils;

import com.thepigcat.transportlib.api.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import java.util.function.Consumer;

public class NetworkHelper {
    public static <T> void iterBlocksBetweenNodes(NetworkNode<T> node0, NetworkNode<T> node1, Consumer<BlockPos> iterator) {
        BlockPos pos0 = node0.getPos();
        BlockPos pos1 = node1.getPos();

        Vec3i delta = pos1.subtract(pos0);
        Direction direction = vecGetDirection(delta);

        int distance = Math.abs(vecEliminateZero(delta)) - 1;

        for (int i = 1; i <= distance; i++) {
            iterator.accept(pos0.relative(direction, i));
        }
    }

    /**
     * Get the only non-zero value in a vector with only one number value
     * @param vec Vector with 2 zero values and only one proper number value
     * @return the number value
     */
    public static int vecEliminateZero(Vec3i vec) {
        if (vec.getX() != 0) return vec.getX();
        if (vec.getY() != 0) return vec.getY();
        if (vec.getZ() != 0) return vec.getZ();
        throw new IllegalStateException("Vector has more than one non-zero number");
    }

    public static Direction vecGetDirection(Vec3i vec) {
        int x = Integer.signum(vec.getX());
        int y = Integer.signum(vec.getY());
        int z = Integer.signum(vec.getZ());
        return Direction.fromDelta(x, y, z);
    }

}
