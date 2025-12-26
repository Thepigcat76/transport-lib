package com.thepigcat.transportlib.api;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface Transporting<T> {
    void setValue(@Nullable T value);

    @Nullable T getValue();

    @Nullable T removeValue();

    void trySyncValue(BlockPos pos);

    TransportNetwork<T> getNetwork();

}
