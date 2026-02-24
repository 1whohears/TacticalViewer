package com.onewhohears.tacview.client.core;

import org.jetbrains.annotations.NotNull;

public class TVClientManager {

    public void tick() {

    }

    public void requestRecordingSessionFromServer(@NotNull String sessionId) {

    }

    private static TVClientManager INSTANCE = null;

    public static TVClientManager get() {
        if (INSTANCE == null) INSTANCE = new TVClientManager();
        return INSTANCE;
    }

    private TVClientManager() {}

}
