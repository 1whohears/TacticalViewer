package com.onewhohears.tacview.common.network.toserver;

import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import com.onewhohears.tacview.common.network.toclient.ToClientSendSession;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ToServerRequestSession extends BaseC2SMessage {

    @NotNull private final String sessionId;

    public ToServerRequestSession(@NotNull String sessionId) {
        this.sessionId = sessionId;
    }

    public ToServerRequestSession(FriendlyByteBuf buffer) {
        this.sessionId = buffer.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getEnvironment().toPlatform() != EnvType.SERVER) return;
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            RecordingSession session = SessionManager.get().getSession(sessionId);
            new ToClientSendSession(sessionId, session).sendTo(player);
        });
    }

    @Override
    public MessageType getType() {
        return TVPacketHandler.C2S_REQUEST_SESSION;
    }

}
