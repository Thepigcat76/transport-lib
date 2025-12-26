package com.thepigcat.transportlib.api;

import net.minecraft.core.BlockPos;

public interface NetworkNode {
    void initialize();

    TransportNetwork<?> getNetwork();

    BlockPos getPos();
}
