package com.thepigcat.transportlib.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface InteractorCheckFunction {
    boolean test(Level level, BlockPos cablePos, BlockPos interactorPos, Direction direction);
}