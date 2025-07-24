package com.thepigcat.transportlib;

import com.thepigcat.transportlib.example.ExampleBlockEntityRegistry;
import com.thepigcat.transportlib.example.client.ManaBatteryBERenderer;
import net.minecraft.SharedConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = TransportLib.MODID, dist = Dist.CLIENT)
public class TransportLibClient {
    public TransportLibClient(IEventBus modEventBus) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            modEventBus.addListener(this::registerBER);
        }
    }

    private void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ExampleBlockEntityRegistry.MANA_BATTERY.get(), ManaBatteryBERenderer::new);
    }
}
