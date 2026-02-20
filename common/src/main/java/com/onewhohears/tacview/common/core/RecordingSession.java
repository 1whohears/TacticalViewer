package com.onewhohears.tacview.common.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecordingSession {

    private final String sessionId;
    private final Map<UUID,EntityRecorder> RECORDERS = new HashMap<>();
    private final int defaultRecordRate;
    private final int maxLength;
    private int length = 0;
    private long sessionStartTime = -1;
    private boolean recordingComplete = false;

    public void addEntityToRecord(@NotNull Entity entity) {
        if (RECORDERS.containsKey(entity.getUUID())) return;
        RECORDERS.put(entity.getUUID(), EntityRecorders.createEntityRecorder(entity, getEntityRecordRate(entity)));
    }

    public void tickRecord(@NotNull ServerLevel level) {
        if (recordingComplete) return;
        long currentTime = level.getGameTime();
        if (sessionStartTime == -1) sessionStartTime = currentTime;
        RECORDERS.forEach((uuid, recorder) -> recorder.tickRecord(this, level));
        length = Math.toIntExact(currentTime - sessionStartTime);
        if (currentTime - sessionStartTime >= maxLength) finishRecording(level);
    }

    public RecordingSession(@NotNull String sessionId, @NotNull Collection<Entity> entities,
                            int defaultRecordRate, int maxLength) {
        this.sessionId = sessionId;
        if (defaultRecordRate <= 0) defaultRecordRate = 1;
        this.defaultRecordRate = defaultRecordRate;
        if (maxLength <= 0) maxLength = 0;
        this.maxLength = maxLength;
        for (Entity entity : entities) {
            RECORDERS.put(entity.getUUID(), EntityRecorders.createEntityRecorder(entity, defaultRecordRate));
        }
    }

    public RecordingSession(@NotNull JsonObject data) {
        this.sessionId = UtilParse.getStringSafe(data, "sessionId", "");
        this.defaultRecordRate = UtilParse.getIntSafe(data, "defaultRecordRate", 10);
        this.sessionStartTime = UtilParse.getIntSafe(data, "sessionStartTime", -1);
        this.maxLength = UtilParse.getIntSafe(data, "maxLength", -1);
        this.length = UtilParse.getIntSafe(data, "length", -1);
        this.recordingComplete = UtilParse.getBooleanSafe(data, "recordingComplete", false);
        JsonArray recorderArray = !data.has("recorders") ? new JsonArray() : data.get("recorders").getAsJsonArray();
        for (int i = 0; i < recorderArray.size(); ++i) {
            JsonObject recObject = recorderArray.get(i).getAsJsonObject();
            EntityRecorder recorder = EntityRecorders.readEntityRecorder(recObject);
            if (recorder == null) continue;
            RECORDERS.put(recorder.uuid.get(), recorder);
        }
    }

    public JsonObject getSaveData() {
        JsonObject data = new JsonObject();
        data.addProperty("sessionId", sessionId);
        data.addProperty("defaultRecordRate", defaultRecordRate);
        data.addProperty("sessionStartTime", sessionStartTime);
        data.addProperty("maxLength", maxLength);
        data.addProperty("length", length);
        data.addProperty("recordingComplete", recordingComplete);
        JsonArray recorderArray = new JsonArray();
        for (EntityRecorder recorder : RECORDERS.values()) recorderArray.add(recorder.getSaveData());
        data.add("recorders", recorderArray);
        return data;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public int getEntityRecordRate(@NotNull Entity entity) {
        if (UtilEntity.isPlayer(entity)) return 5;
        return defaultRecordRate;
    }

    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    public int getLength() {
        return length;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public boolean isRecordingComplete() {
        return recordingComplete;
    }

    public void finishRecording(@NotNull ServerLevel level) {
        if (isRecordingComplete()) return;
        long currentTime = level.getGameTime();
        length = Math.toIntExact(currentTime - sessionStartTime);
        recordingComplete = true;
        SessionManager.get().saveSessionData(getSessionId(), SessionManager.INFO);
    }

}
