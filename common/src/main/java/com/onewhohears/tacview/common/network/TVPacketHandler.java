package com.onewhohears.tacview.common.network;

import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.common.network.toclient.ToClientSendSession;
import com.onewhohears.tacview.common.network.toclient.ToClientSyncVehiclePos;
import com.onewhohears.tacview.common.network.toserver.ToServerRequestSession;
import com.onewhohears.tacview.common.network.toserver.ToServerSyncVehiclePos;
import com.onewhohears.tacview.common.network.toserver.ToServerUpdateViewer;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TVPacketHandler {

	private TVPacketHandler() {}

    public static final SimpleNetworkManager INSTANCE = SimpleNetworkManager.create(TacViewMod.MOD_ID);

    public static final MessageType C2S_REQUEST_SESSION = INSTANCE.registerC2S(
            "c2s_request_session", ToServerRequestSession::new);
    public static final MessageType C2S_UPDATE_VIEWER = INSTANCE.registerC2S(
            "c2s_update_viewer", ToServerUpdateViewer::new);
    public static final MessageType C2S_SYNC_VEHICLE_POS = INSTANCE.registerC2S(
            "c2s_sync_vehicle_pos", ToServerSyncVehiclePos::new);

    public static final MessageType S2C_SEND_SESSION = INSTANCE.registerS2C(
            "s2c_send_session", ToClientSendSession::new);
    public static final MessageType S2C_SYNC_VEHICLE_POS = INSTANCE.registerS2C(
            "s2c_sync_vehicle_pos", ToClientSyncVehiclePos::new);

    public static LevelChunk getEntityChunk(@NotNull Entity entity) {
        return UtilEntity.getLevel(entity).getChunkAt(entity.blockPosition());
    }

    public static void sendToTrackers(@NotNull BaseS2CMessage message, @Nullable Entity entity) {
        if (entity == null) return;
        message.sendToChunkListeners(getEntityChunk(entity));
    }

    public static void register() {}
	
}
