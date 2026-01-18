package com.thepigcat.transportlib.api;

import com.thepigcat.transportlib.api.cache.NetworkRoute;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface Transporting<T> {
    void setValue(@Nullable T value);

    @Nullable T getValue();

    @Nullable T removeValue();

    void trySyncValue(BlockPos pos);

    NetworkRoute<T> getRoute();

    TransportNetwork<T> getNetwork();

}
