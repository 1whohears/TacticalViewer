package com.onewhohears.tacview.client.core;

import com.google.gson.JsonObject;
import com.onewhohears.tacview.common.core.SessionState;
import com.onewhohears.tacview.common.network.toserver.ToServerRequestSession;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TVClientManager {

    private final Map<String, Long> requestedSessions = new HashMap<>();

    public void tick() {

    }

    /**
     * CLIENT ONLY
     */
    public void requestRecordingSessionFromServer(@NotNull String sessionId) {
        long currentTime = System.currentTimeMillis();
        if (requestedSessions.containsKey(sessionId)) {
            // TODO if recording session not received for a long time then try again
            return;
        }
        requestedSessions.put(sessionId, currentTime);
        new ToServerRequestSession(sessionId).sendToServer();
    }

    public void handleReceiveRecordSession(SessionState sessionState, @NotNull String sessionId,
                                           @NotNull JsonObject sessionData) {
        // TODO tell the client side session manager to load the session data
    }

    private static TVClientManager INSTANCE = null;

    public static TVClientManager get() {
        if (INSTANCE == null) INSTANCE = new TVClientManager();
        return INSTANCE;
    }

    private TVClientManager() {}

}
