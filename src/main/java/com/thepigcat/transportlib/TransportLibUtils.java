package com.thepigcat.transportlib;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;

import java.util.HashMap;
import java.util.Map;

public final class TransportLibUtils {
    public static final Codec<Tag> TAG_CODEC = ExtraCodecs.converter(NbtOps.INSTANCE);

    public static <V> Map<String, V> encodePosMap(Map<BlockPos, V> map) {
        Map<String, V> newMap = new HashMap<>();
        for (Map.Entry<BlockPos, V> entry : map.entrySet()) {
            newMap.put(String.valueOf(entry.getKey().asLong()), entry.getValue());
        }
        return newMap;
    }

    public static <V> Map<BlockPos, V> decodePosMap(Map<String, V> map) {
        Map<BlockPos, V> newMap = new HashMap<>();
        for (Map.Entry<String, V> entry : map.entrySet()) {
            newMap.put(BlockPos.of(Long.parseLong(entry.getKey())), entry.getValue());
        }
        return newMap;
    }
}
