package com.onewhohears.tacview.client.core;

import com.google.gson.JsonObject;
import com.onewhohears.tacview.client.overlay.PlaybackInfoOverlay;
import com.onewhohears.tacview.common.command.TacViewCommands;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.core.SessionState;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import com.onewhohears.tacview.common.network.toserver.ToServerRequestSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TVClientManager {

    public static final long REQUEST_RETRY_TIME = 4000;
    private final Map<String, RequestedSessionData> requestedSessions = new HashMap<>();

    public void tick() {
        // TODO use hotkeys to manipulate the replay
        handleOverlay();
    }

    protected void handleOverlay() {
        Minecraft m = Minecraft.getInstance();
        if (m.player == null || m.level == null) {
            PlaybackInfoOverlay.setOverlayTarget(null);
            return;
        }
        if (m.level.getGameTime() % 10 != 0) return;
        AABB aabb = AABB.ofSize(m.player.position(), 1, 1, 1).inflate(16);
        List<TacViewEntity> list = m.level.getEntitiesOfClass(TacViewEntity.class, aabb);
        TacViewEntity entity = TacViewCommands.findClosestEntity(m.player.position(), list);
        if (entity == null) {
            PlaybackInfoOverlay.setOverlayTarget(null);
            return;
        }
        PlaybackInfoOverlay.setOverlayTarget(entity.playback);
    }

    /**
     * CLIENT ONLY
     */
    public void requestRecordingSessionFromServer(@NotNull String sessionId) {
        if (requestedSessions.containsKey(sessionId)) {
            RequestedSessionData reqData = requestedSessions.get(sessionId);
            switch (reqData.getSessionState()) {
                case COMPLETE, NOT_EXIST -> {
                    return;
                }
                case REQUESTED, NOT_FINISHED -> {
                    long timeDiff = System.currentTimeMillis() - reqData.getUpdateTime();
                    if (timeDiff < REQUEST_RETRY_TIME) {
                        return;
                    }
                }
            }
        }
        requestedSessions.put(sessionId, new RequestedSessionData(sessionId, SessionState.REQUESTED));
        new ToServerRequestSession(sessionId).sendToServer();
    }

    public void handleReceiveRecordSession(SessionState sessionState, @NotNull String sessionId,
                                           @NotNull JsonObject sessionData) {
        if (!requestedSessions.containsKey(sessionId)) {
            requestedSessions.put(sessionId, new RequestedSessionData(sessionId, sessionState));
        }
        RequestedSessionData reqData = requestedSessions.get(sessionId);
        reqData.updateState(sessionState);
        if (sessionState == SessionState.COMPLETE) {
            SessionManager.get().readSessionDataFromServer(sessionData);
        }
    }

    public static class RequestedSessionData {
        private final String sessionId;
        private long updateTime;
        private SessionState state;
        public RequestedSessionData(@NotNull String sessionId, SessionState state) {
            this.sessionId = sessionId;
            this.updateTime = System.currentTimeMillis();
            this.state = state;
        }
        public SessionState getSessionState() {
            return state;
        }
        public void updateState(SessionState state) {
            this.state = state;
            updateTime = System.currentTimeMillis();
        }
        @NotNull
        public String getSessionId() {
            return sessionId;
        }
        public long getUpdateTime() {
            return updateTime;
        }
    }

    private static TVClientManager INSTANCE = null;

    public static TVClientManager get() {
        if (INSTANCE == null) INSTANCE = new TVClientManager();
        return INSTANCE;
    }

    private TVClientManager() {}

}
