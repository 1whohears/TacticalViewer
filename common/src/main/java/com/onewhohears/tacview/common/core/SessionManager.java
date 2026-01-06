package com.onewhohears.tacview.common.core;

import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    public static String SESSION_PATH = "tac_view/recordings/";

    private final Map<String, RecordingSession> SESSIONS = new HashMap<>();

    public boolean startNewSession(String sessionId, @NotNull Collection<Entity> entities,
                                   int defaultRecordRate, int length) {
        if (SESSIONS.containsKey(sessionId)) return false;
        if (UtilFile.doesFileExistGamePath(getSessionFileName(sessionId))) return false;
        RecordingSession session = new RecordingSession(sessionId, entities, defaultRecordRate, length);
        SESSIONS.put(sessionId, session);
        return true;
    }

    public void tickRecord(@NotNull ServerLevel level) {
        SESSIONS.forEach((id, session) -> session.tickRecord(level));
    }

    @Nullable
    public RecordingSession getSession(String id) {
        return SESSIONS.get(id);
    }

    public void readSessionData(String sessionId) {

    }

    public void saveSessionData(String sessionId) {

    }

    private static SessionManager INSTANCE = null;

    public static SessionManager get() {
        if (INSTANCE == null) INSTANCE = new SessionManager();
        return INSTANCE;
    }

    public static String getSessionFileName(String sessionId) {
        return SESSION_PATH + sessionId + ".json";
    }

    private SessionManager() {}

}
