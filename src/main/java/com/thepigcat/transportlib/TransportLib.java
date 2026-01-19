package com.thepigcat.transportlib;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.thepigcat.transportlib.api.NetworkNode;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.client.ClientNodes;
import com.thepigcat.transportlib.client.debug.TransportNetworkRenderer;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import com.thepigcat.transportlib.impl.data.NodeNetworkData;
import com.thepigcat.transportlib.impl.data.TLServerRouteCache;
import com.thepigcat.transportlib.example.ExampleBlockEntityRegistry;
import com.thepigcat.transportlib.example.ExampleBlockRegistry;
import com.thepigcat.transportlib.example.ExampleItemRegistry;
import com.thepigcat.transportlib.example.ExampleNetworkRegistry;
import com.thepigcat.transportlib.networking.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
import java.util.stream.Collectors;

@Mod(TransportLib.MODID)
public final class TransportLib {
    public static final String MODID = "transport_lib";
    public static final Registry<TransportNetwork<?>> NETWORK_REGISTRY = new RegistryBuilder<TransportNetwork<?>>(ResourceKey.createRegistryKey(rl("network"))).create();
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

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void registerRegistry(NewRegistryEvent event) {
        event.register(NETWORK_REGISTRY);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(AddNetworkNodePayload.TYPE, AddNetworkNodePayload.STREAM_CODEC, AddNetworkNodePayload::handle);
        for (TransportNetwork<?> network : NETWORK_REGISTRY) {
            registrar.playToClient(RemoveNetworkNodePayload.TYPE, RemoveNetworkNodePayload.STREAM_CODEC, RemoveNetworkNodePayload::handle);
            registrar.playToClient(SyncNetworkNodePayload.TYPE, SyncNetworkNodePayload.STREAM_CODEC, SyncNetworkNodePayload::handle);
        }

        registrar.playToClient(AddNextNodePayload.TYPE, AddNextNodePayload.STREAM_CODEC, AddNextNodePayload::handle);
        registrar.playToClient(RemoveNextNodePayload.TYPE, RemoveNextNodePayload.STREAM_CODEC, RemoveNextNodePayload::handle);
        registrar.playToClient(SyncNextNodePayload.TYPE, SyncNextNodePayload.STREAM_CODEC, SyncNextNodePayload::handle);

        registrar.playToClient(AddInteractorPayload.TYPE, AddInteractorPayload.STREAM_CODEC, AddInteractorPayload::handle);
        registrar.playToClient(RemoveInteractorPayload.TYPE, RemoveInteractorPayload.STREAM_CODEC, RemoveInteractorPayload::handle);
        registrar.playToClient(SyncInteractorPayload.TYPE, SyncInteractorPayload.STREAM_CODEC, SyncInteractorPayload::handle);
        registrar.playToClient(SetNodeValuePayload.TYPE, SetNodeValuePayload.STREAM_CODEC, SetNodeValuePayload::handle);
        registrar.playToClient(ClearClientCachePayload.TYPE, ClearClientCachePayload.STREAM_CODEC, ClearClientCachePayload::handle);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, ClearClientCachePayload.INSTANCE);
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
        Map<BlockPos, NetworkNode<T>> networkData = TransportNetworkImpl.getRawNetworkData(network, serverPlayer.serverLevel()).nodes();
        Map<BlockPos, Tag> map = networkData.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), NetworkNode.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        PacketDistributor.sendToPlayer(serverPlayer, new SyncNetworkNodePayload(network, new HashMap<>(map)));
        PacketDistributor.sendToPlayer(serverPlayer, new SyncNextNodePayload(network));
    }

}
