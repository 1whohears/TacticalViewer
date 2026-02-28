package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import com.onewhohears.tacview.init.TVModEntities;
import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SessionManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Consumer<String> INFO = LOGGER::info;

    public static String SESSION_PATH = "tac_view/recordings/";

    private final Map<String,RecordingSession> SESSIONS = new HashMap<>();

    public boolean watchReplay(@NotNull TacViewEntity entity, @NotNull String sessionId,
                               @NotNull Consumer<String> debug) {
        if (!SESSIONS.containsKey(sessionId)) {
            debug.accept("Could not start playback for session "+sessionId
                    +" because the session is not loaded or does not exist!");
            return false;
        }
        if (!SESSIONS.get(sessionId).isRecordingComplete()) {
            debug.accept("Could not start playback for session "+sessionId
                    +" because the session has not finished recording!");
            return false;
        }
        entity.setSessionId(sessionId);
        entity.resetReplay();
        debug.accept("Started playback for session "+sessionId+" at "+entity.position());
        return true;
    }

    public boolean createViewer(@NotNull ServerLevel level, @NotNull Vec3 pos, float width, float height,
                                @NotNull Consumer<String> debug) {
        TacViewEntity entity = TVModEntities.TAC_VIEW.get().create(level);
        if (entity == null) {
            debug.accept("Failed to create a viewer entity.");
            return false;
        }
        entity.setPos(pos);
        entity.setWidth(width);
        entity.setHeight(height);
        level.addFreshEntity(entity);
        debug.accept("Created a new viewer at "+pos);
        return true;
    }

    public boolean startNewSession(@NotNull String sessionId, @NotNull Collection<? extends Entity> entities,
                                   @NotNull ServerLevel level, int defaultRecordRate, int length,
                                   @NotNull Consumer<String> debug) {
        if (SESSIONS.containsKey(sessionId)) {
            debug.accept("Cannot start new session because a session with id "+sessionId+" already exists.");
            return false;
        }
        if (UtilFile.doesFileExistGamePath(getSessionFileName(sessionId))) {
            debug.accept("Cannot start new session because an unloaded session file with id "+sessionId+" already exists.");
            return false;
        }
        RecordingSession session = new RecordingSession(sessionId, entities, defaultRecordRate, length, level);
        SESSIONS.put(sessionId, session);
        debug.accept("Started new recording session "+sessionId+" it will end in "+length+" ticks!");
        return true;
    }

    public void tickRecord(@NotNull ServerLevel level) {
        SESSIONS.forEach((id, session) -> session.tickRecord(level));
    }

    @Nullable
    public RecordingSession getSession(@NotNull String id) {
        return SESSIONS.get(id);
    }

    public Set<String> getLoadedSessionIds() {
        return SESSIONS.keySet();
    }

    public Set<String> getUnloadedSessionIds() {
        Set<String> ids = UtilFile.getJsonFileNamesInGamePath(SESSION_PATH);
        ids.removeAll(getLoadedSessionIds());
        return ids;
    }

    public boolean readSessionData(@NotNull String sessionId, @NotNull Consumer<String> debug) {
        if (SESSIONS.containsKey(sessionId)) {
            debug.accept("Could not load recording session "+sessionId+" because it was already loaded!");
            return false;
        }
        JsonObject sessionJson = UtilFile.readJsonGamePath(getSessionFileName(sessionId));
        if (!sessionJson.has("sessionId")) {
            debug.accept("Could not load recording session "+sessionId+" because there is no file.");
            return false;
        }
        RecordingSession session = new RecordingSession(sessionJson);
        if (!sessionId.equals(session.getSessionId())) {
            debug.accept("Could not load recording session "+sessionId+" because of id mismatch!");
            return false;
        }
        SESSIONS.put(sessionId, session);
        debug.accept("Loaded Recording Session "+sessionId);
        return true;
    }

    public boolean saveSessionData(@NotNull String sessionId, @NotNull Consumer<String> debug) {
        RecordingSession session = getSession(sessionId);
        if (session == null) {
            debug.accept("Could not save session "+sessionId+" because it has not been loaded or doesn't exist!");
            return false;
        }
        JsonObject sessionJson = session.getSaveData();
        UtilFile.printGamePath(getSessionFileName(sessionId), sessionJson);
        debug.accept("Saved Recording Session "+sessionId);
        return true;
    }

    public boolean unloadSession(@NotNull String sessionId, @NotNull Consumer<String> debug) {
        if (!saveSessionData(sessionId, debug)) {
            debug.accept("Could not unload session "+sessionId+" because it could not be saved!");
            return false;
        }
        SESSIONS.remove(sessionId);
        debug.accept("Unloaded Recording Session "+sessionId);
        return true;
    }

    public boolean stopRecording(@NotNull String sessionId, @NotNull ServerLevel level,
                                 @NotNull Consumer<String> debug) {
        if (!SESSIONS.containsKey(sessionId)) {
            debug.accept("Could not stop recording session "+sessionId+" because it is not loaded!");
            return false;
        }
        RecordingSession session = SESSIONS.get(sessionId);
        return session.finishRecording(level, debug);
    }

    public void readSessionDataFromServer(@NotNull JsonObject sessionData) {
        RecordingSession session = new RecordingSession(sessionData);
        SESSIONS.put(session.getSessionId(), session);
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
