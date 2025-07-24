package com.thepigcat.transportlib.example;

import com.thepigcat.transportlib.TransportLib;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExampleItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TransportLib.MODID);

    public static final DeferredItem<BlockItem> MANA_PIPE = ITEMS.register("mana_pipe",
            () -> new BlockItem(ExampleBlockRegistry.MANA_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MANA_BATTERY = ITEMS.register("mana_battery",
            () -> new BlockItem(ExampleBlockRegistry.MANA_BATTERY.get(), new Item.Properties()));

}
