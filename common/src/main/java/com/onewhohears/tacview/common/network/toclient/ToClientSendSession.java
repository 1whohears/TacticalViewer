package com.onewhohears.tacview.common.network.toclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onewhohears.tacview.client.core.TVClientManager;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionState;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ToClientSendSession extends BaseS2CMessage {

    private final SessionState sessionState;
    @NotNull private final String sessionId;
    @NotNull private final JsonObject sessionData;

    public ToClientSendSession(@NotNull String sessionId, @Nullable RecordingSession session) {
        this.sessionId = sessionId;
        if (session == null) {
            sessionState = SessionState.NOT_EXIST;
            sessionData = new JsonObject();
        } else if (!session.isRecordingComplete()) {
            sessionState = SessionState.NOT_FINISHED;
            sessionData = new JsonObject();
        } else {
            sessionState = SessionState.COMPLETE;
            sessionData = session.getSaveData();
        }
    }

    public ToClientSendSession(FriendlyByteBuf buffer) {
        sessionState = buffer.readEnum(SessionState.class);
        sessionId = buffer.readUtf();
        byte[] compressed = buffer.readByteArray();
        GZIPInputStream gzip = null;
        String json = null;
        try {
            gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
            json = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sessionData = JsonParser.parseString(json).getAsJsonObject();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(sessionState);
        buffer.writeUtf(sessionId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(baos);
            gzip.write(sessionData.toString().getBytes(StandardCharsets.UTF_8));
            gzip.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buffer.writeByteArray(baos.toByteArray());
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getEnvironment().toPlatform() != EnvType.CLIENT) return;
            TVClientManager.get().handleReceiveRecordSession(sessionState, sessionId, sessionData);
        });
    }

    @Override
    public MessageType getType() {
        return TVPacketHandler.S2C_SEND_SESSION;
    }


}
