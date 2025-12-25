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

    private final Map<UUID, EntityRecorder> RECORDERS = new HashMap<>();
    private final int defaultRecordRate;
    private long sessionStartTime = -1;

    public void addEntityToRecord(@NotNull Entity entity) {
        if (RECORDERS.containsKey(entity.getUUID())) return;
        RECORDERS.put(entity.getUUID(), EntityRecorders.createEntityRecorder(entity, getEntityRecordRate(entity)));
    }

    public void tickRecord(@NotNull ServerLevel level) {
        if (sessionStartTime == -1) sessionStartTime = level.getGameTime();
        RECORDERS.forEach((uuid, recorder) -> recorder.tickRecord(this, level));
    }

    public RecordingSession(@NotNull Collection<Entity> entities, int defaultRecordRate) {
        this.defaultRecordRate = defaultRecordRate;
        for (Entity entity : entities) {
            RECORDERS.put(entity.getUUID(), EntityRecorders.createEntityRecorder(entity, defaultRecordRate));
        }
    }

    public RecordingSession(@NotNull JsonObject data) {
        this.defaultRecordRate = UtilParse.getIntSafe(data, "defaultRecordRate", 10);
        this.sessionStartTime = UtilParse.getIntSafe(data, "sessionStartTime", -1);
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
        data.addProperty("defaultRecordRate", defaultRecordRate);
        data.addProperty("sessionStartTime", sessionStartTime);
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

}
