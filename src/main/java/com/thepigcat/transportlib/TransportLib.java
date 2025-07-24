package com.thepigcat.transportlib;

import com.mojang.logging.LogUtils;
import com.thepigcat.transportlib.api.transportation.NetworkNode;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import com.thepigcat.transportlib.client.transportation.debug.TransportNetworkRenderer;
import com.thepigcat.transportlib.data.TLServerRouteCache;
import com.thepigcat.transportlib.example.ExampleBlockEntityRegistry;
import com.thepigcat.transportlib.example.ExampleBlockRegistry;
import com.thepigcat.transportlib.example.ExampleItemRegistry;
import com.thepigcat.transportlib.example.ExampleNetworkRegistry;
import com.thepigcat.transportlib.networking.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

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

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

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

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            for (TransportNetwork<?> network : NETWORK_REGISTRY) {
                if (network.isSynced()) {
                    sendSyncPayload(network, serverPlayer);
                }
            }
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        TLServerRouteCache.CACHE.clear();
    }

    private void onServerStopped(ServerStoppedEvent event) {
        TLServerRouteCache.CACHE.clear();
    }

    private <T> void sendSyncPayload(TransportNetwork<T> network, ServerPlayer serverPlayer) {
        Map<BlockPos, NetworkNode<T>> serverNodes = network.getServerNodes(serverPlayer.serverLevel());
        PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkNodePayload<>(network, new HashMap<>(serverNodes)));
        PacketDistributor.sendToPlayer(serverPlayer, new SyncNextNodePayload(network));
    }

}
