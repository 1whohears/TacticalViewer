package com.onewhohears.tacview.common.network.toclient;

import com.onewhohears.tacview.client.core.TVClientManager;
import com.onewhohears.tacview.common.core.SaveStateManager;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class ToClientSyncVehiclePos extends BaseS2CMessage {

    private final SaveStateManager.VehicleSyncData data;

    public ToClientSyncVehiclePos(@NotNull SaveStateManager.VehicleSyncData data) {
        this.data = data;
    }

    public ToClientSyncVehiclePos(FriendlyByteBuf buffer) {
        this.data = new SaveStateManager.VehicleSyncData();
        this.data.read(buffer);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        this.data.write(buffer);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getEnvironment().toPlatform() != EnvType.CLIENT) return;
            TVClientManager.get().handleSyncVehiclePos(data);
        });
    }

    @Override
    public MessageType getType() {
        return TVPacketHandler.S2C_SYNC_VEHICLE_POS;
    }


}
