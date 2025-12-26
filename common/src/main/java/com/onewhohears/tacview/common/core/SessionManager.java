package com.onewhohears.tacview.common.core;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    private final Map<String, RecordingSession> SESSIONS = new HashMap<>();

    public boolean startNewSession(String sessionId, @NotNull Collection<Entity> entities,
                                   int defaultRecordRate, int length) {

    }

    private static SessionManager INSTANCE = null;

    public static SessionManager get() {
        if (INSTANCE == null) INSTANCE = new SessionManager();
        return INSTANCE;
    }

    private SessionManager() {}

}
