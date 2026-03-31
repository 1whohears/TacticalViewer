package com.onewhohears.tacview.common.network.toserver;

import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SaveStateManager;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import com.onewhohears.tacview.common.network.toclient.ToClientSendSession;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ToServerSyncVehiclePos extends BaseC2SMessage {

    private final SaveStateManager.VehicleSyncData data;

    public ToServerSyncVehiclePos(@NotNull SaveStateManager.VehicleSyncData data) {
        this.data = data;
    }

    public ToServerSyncVehiclePos(FriendlyByteBuf buffer) {
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
            if (context.getEnvironment().toPlatform() != EnvType.SERVER) return;
            SaveStateManager.get().handleSyncVehicleReturn((ServerLevel) UtilEntity.getLevel(context.getPlayer()), data);
        });
    }

    @Override
    public MessageType getType() {
        return TVPacketHandler.C2S_SYNC_VEHICLE_POS;
    }

}
