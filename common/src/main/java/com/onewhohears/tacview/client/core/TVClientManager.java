package com.onewhohears.tacview.client.core;

import com.google.gson.JsonObject;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.core.SessionState;
import com.onewhohears.tacview.common.network.toserver.ToServerRequestSession;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TVClientManager {

    public static final long REQUEST_RETRY_TIME = 4000;
    private final Map<String, RequestedSessionData> requestedSessions = new HashMap<>();

    public void tick() {
        // TODO use hotkeys to manipulate the replay
        // TODO view entity data when mouse points at them
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
