package com.thepigcat.transportlib.example;

import com.mojang.serialization.Codec;
import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.api.transportation.NetworkNode;
import com.thepigcat.transportlib.api.transportation.TransferSpeed;
import com.thepigcat.transportlib.api.transportation.TransportNetwork;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExampleNetworkRegistry {
    public static final DeferredRegister<TransportNetwork<?>> NETWORKS = DeferredRegister.create(TransportLib.NETWORK_REGISTRY, TransportLib.MODID);

    public static final DeferredHolder<TransportNetwork<?>, TransportNetwork<Integer>> MANA_NETWORK = NETWORKS.register("mana",
            () -> TransportNetwork.builder(NetworkNode::new, Codec.INT, ManaTransportingHandler.INSTANCE)
                    .synced(ByteBufCodecs.INT)
                    .lossPerBlock((level, node) -> 1f)
                    .transferSpeed(TransferSpeed::instant)
                    // FIXME: Look at the builder for actual fixme (sus positioning)
                    .interactorCheck(((level, pos, direction) -> level.getBlockEntity(pos.relative(direction)) instanceof ManaBatteryBlockEntity))
                    .build());
}
