package com.thepigcat.transportlib;

import com.mojang.logging.LogUtils;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import com.thepigcat.transportlib.client.transportation.debug.TransportNetworkRenderer;
import com.thepigcat.transportlib.example.ExampleBlockEntityRegistry;
import com.thepigcat.transportlib.example.ExampleBlockRegistry;
import com.thepigcat.transportlib.example.ExampleItemRegistry;
import com.thepigcat.transportlib.example.ExampleNetworkRegistry;
import com.thepigcat.transportlib.networking.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TransportLib.MODID)
public final class TransportLib {
    public static final String MODID = "transport_lib";
    public static final Registry<TransportNetwork<?>> NETWORK_REGISTRY = new RegistryBuilder<TransportNetwork<?>>(ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MODID, "network"))).create();
    public static final Logger LOGGER = LogUtils.getLogger();

    public TransportLib(IEventBus modEventbus, ModContainer modContainer) {
        modEventbus.addListener(this::registerPayloads);
        modEventbus.addListener(this::registerRegistry);

        if (SharedConstants.IS_RUNNING_IN_IDE) {
            ExampleNetworkRegistry.NETWORKS.register(modEventbus);
            ExampleBlockRegistry.BLOCKS.register(modEventbus);
            ExampleItemRegistry.ITEMS.register(modEventbus);
            ExampleBlockEntityRegistry.BLOCK_ENTITIES.register(modEventbus);

            NeoForge.EVENT_BUS.addListener(TransportNetworkRenderer::renderNetworkNodes);
        }
    }

    private void registerRegistry(NewRegistryEvent event) {
        event.register(NETWORK_REGISTRY);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        for (TransportNetwork<?> network : NETWORK_REGISTRY) {
            registrar.playToClient(AddNetworkNodePayload.type(network), AddNetworkNodePayload.streamCodec(network), AddNetworkNodePayload::handle);
            registrar.playToClient(RemoveNetworkNodePayload.type(network), RemoveNetworkNodePayload.streamCodec(network), RemoveNetworkNodePayload::handle);
            registrar.playToClient(SyncNetworkNodePayload.type(network), SyncNetworkNodePayload.streamCodec(network), SyncNetworkNodePayload::handle);
        }

        registrar.playToClient(AddNextNodePayload.TYPE, AddNextNodePayload.STREAM_CODEC, AddNextNodePayload::handle);
        registrar.playToClient(RemoveNextNodePayload.TYPE, RemoveNextNodePayload.STREAM_CODEC, RemoveNextNodePayload::handle);
        registrar.playToClient(SyncNextNodePayload.TYPE, SyncNextNodePayload.STREAM_CODEC, SyncNextNodePayload::handle);

        registrar.playToClient(AddInteractorPayload.TYPE, AddInteractorPayload.STREAM_CODEC, AddInteractorPayload::handle);
        registrar.playToClient(RemoveInteractorPayload.TYPE, RemoveInteractorPayload.STREAM_CODEC, RemoveInteractorPayload::handle);
        registrar.playToClient(SyncInteractorPayload.TYPE, SyncInteractorPayload.STREAM_CODEC, SyncInteractorPayload::handle);
    }

}
