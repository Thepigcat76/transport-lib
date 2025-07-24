package com.thepigcat.transportlib.example;

import com.thepigcat.transportlib.TransportLib;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExampleBlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TransportLib.MODID);

    public static final DeferredBlock<ManaPipeBlock> MANA_PIPE = BLOCKS.register("mana_pipe", () -> new ManaPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), 6));
    public static final DeferredBlock<ManaBatteryBlock> MANA_BATTERY = BLOCKS.register("mana_battery", () -> new ManaBatteryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
}
