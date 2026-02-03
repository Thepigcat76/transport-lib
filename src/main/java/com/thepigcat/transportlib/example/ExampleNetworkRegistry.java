package com.thepigcat.transportlib.example;

import com.thepigcat.transportlib.TransportLib;
import com.thepigcat.transportlib.impl.NetworkNodeImpl;
import com.thepigcat.transportlib.api.TransferSpeed;
import com.thepigcat.transportlib.api.TransportNetwork;
import com.thepigcat.transportlib.impl.TransportNetworkImpl;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExampleNetworkRegistry {
    public static final DeferredRegister<TransportNetwork<?>> NETWORKS = DeferredRegister.create(TransportLib.NETWORK_REGISTRY, TransportLib.MODID);

    public static final DeferredHolder<TransportNetwork<?>, TransportNetworkImpl<Integer>> MANA_NETWORK = NETWORKS.register("mana",
            () -> TransportNetworkImpl.builder(ManaTransportingHandler.INSTANCE)
                    .synced(ByteBufCodecs.INT)
                    .lossPerBlock((level, node) -> 1f)
                    .transferSpeed(TransferSpeed::instant)
                    // FIXME: Look at the builder for actual fixme (sus positioning)
                    .interactorCheck(((level, cablePos, interactorPos, direction) -> level.getBlockEntity(interactorPos) instanceof ManaBatteryBlockEntity))
                    .build());
}
