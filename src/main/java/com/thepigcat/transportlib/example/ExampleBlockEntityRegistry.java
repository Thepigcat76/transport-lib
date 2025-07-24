package com.thepigcat.transportlib.example;

import com.thepigcat.transportlib.TransportLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExampleBlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TransportLib.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaBatteryBlockEntity>> MANA_BATTERY = BLOCK_ENTITIES.register("mana_battery",
            () -> BlockEntityType.Builder.of(ManaBatteryBlockEntity::new, ExampleBlockRegistry.MANA_BATTERY.get())
                    .build(null));
}
