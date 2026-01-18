package com.thepigcat.transportlib.api;

import com.thepigcat.transportlib.impl.data.NodeNetworkData;
import com.thepigcat.transportlib.impl.data.NodeNetworkSavedData;
import net.minecraft.server.level.ServerLevel;

public class NetworkTicker {
    private final ServerLevel serverLevel;
    private final NodeNetworkSavedData savedData;

    public NetworkTicker(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
        this.savedData = NodeNetworkSavedData.getNetworkData(serverLevel);
    }

    public void tick() {
        for (NodeNetworkData<?> networkNodes : this.savedData.getData().values()) {
            for (NetworkNode<?> node : networkNodes.nodes().values()) {
                node.tick();
            }
        }
    }
}
