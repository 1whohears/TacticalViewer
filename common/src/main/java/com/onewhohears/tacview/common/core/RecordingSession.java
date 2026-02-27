package com.onewhohears.tacview.common.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RecordingSession {

    private final String sessionId;
    private final Map<UUID,EntityRecorder> RECORDERS = new HashMap<>();
    private final int defaultRecordRate;
    private final int maxLength;
    private final ResourceKey<Level> dimension;
    private int length = 0;
    private long sessionStartTime = -1;
    private boolean recordingComplete = false;
    private Vec3 minBound = Vec3.ZERO, maxBound = Vec3.ZERO;

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
        if (currentTime - sessionStartTime >= maxLength) finishRecording(level, SessionManager.INFO);
    }

    public void forEachRecorder(BiConsumer<UUID,EntityRecorder> consumer) {
        RECORDERS.forEach(consumer);
    }

    public RecordingSession(@NotNull String sessionId, @NotNull Collection<? extends Entity> entities,
                            int defaultRecordRate, int maxLength, @NotNull ServerLevel level) {
        this.sessionId = sessionId;
        if (defaultRecordRate <= 0) defaultRecordRate = 1;
        this.defaultRecordRate = defaultRecordRate;
        if (maxLength <= 0) maxLength = 0;
        this.maxLength = maxLength;
        for (Entity entity : entities) {
            RECORDERS.put(entity.getUUID(), EntityRecorders.createEntityRecorder(entity, defaultRecordRate));
        }
        this.dimension = level.dimension();
    }

    public RecordingSession(@NotNull JsonObject data) {
        this.sessionId = UtilParse.getStringSafe(data, "sessionId", "");
        this.defaultRecordRate = UtilParse.getIntSafe(data, "defaultRecordRate", 10);
        this.sessionStartTime = UtilParse.getIntSafe(data, "sessionStartTime", -1);
        this.maxLength = UtilParse.getIntSafe(data, "maxLength", -1);
        this.length = UtilParse.getIntSafe(data, "length", -1);
        this.recordingComplete = UtilParse.getBooleanSafe(data, "recordingComplete", false);
        this.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(
                UtilParse.getStringSafe(data, "dimension", "minecraft:overworld")));
        JsonArray recorderArray = !data.has("recorders") ? new JsonArray() : data.get("recorders").getAsJsonArray();
        for (int i = 0; i < recorderArray.size(); ++i) {
            JsonObject recObject = recorderArray.get(i).getAsJsonObject();
            EntityRecorder recorder = EntityRecorders.readEntityRecorder(recObject);
            if (recorder == null) continue;
            RECORDERS.put(recorder.uuid.get(), recorder);
        }
        this.minBound = UtilParse.readVec3(data, "minBound");
        this.maxBound = UtilParse.readVec3(data, "maxBound");
    }

    public JsonObject getSaveData() {
        JsonObject data = new JsonObject();
        data.addProperty("sessionId", sessionId);
        data.addProperty("defaultRecordRate", defaultRecordRate);
        data.addProperty("sessionStartTime", sessionStartTime);
        data.addProperty("maxLength", maxLength);
        data.addProperty("length", length);
        data.addProperty("recordingComplete", recordingComplete);
        data.addProperty("dimension", dimension.location().toString());
        JsonArray recorderArray = new JsonArray();
        for (EntityRecorder recorder : RECORDERS.values()) recorderArray.add(recorder.getSaveData());
        data.add("recorders", recorderArray);
        UtilParse.writeVec3(data, "minBound", minBound);
        UtilParse.writeVec3(data, "maxBound", maxBound);
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

    public boolean finishRecording(@NotNull ServerLevel level, @NotNull Consumer<String> debug) {
        if (isRecordingComplete()) {
            debug.accept("Could not finish the recording "+sessionId+" because the recording already finished!");
            return false;
        }
        if (!level.dimension().equals(getDimension())) {
            debug.accept("Could not finish the recording "+sessionId+" because" +
                    " you are not in the same dimension as the recording "+getDimension().location());
            return false;
        }
        long currentTime = level.getGameTime();
        length = Math.toIntExact(currentTime - sessionStartTime);
        recordingComplete = true;
        SessionManager.get().saveSessionData(getSessionId(), SessionManager.INFO);
        debug.accept("Finished recording "+sessionId+"! The recording is "+length+" ticks long!");
        return true;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    public String toString() {
        return "[ID]"+getSessionId()
                +"|[LENGTH/MAX]"+getLength()+"/"+getMaxLength()
                +"|[FINISHED]"+isRecordingComplete()
                +"|[RECORDERS]"+RECORDERS.size()
                +"|[DIMENSION]"+getDimension().location();
    }

    public Vec3 getMinBound() {
        return minBound;
    }

    public Vec3 getMaxBound() {
        return maxBound;
    }

    public void updatePosBounds(Vec3 pos) {
        if (minBound.equals(Vec3.ZERO) && maxBound.equals(Vec3.ZERO)) {
            minBound = pos;
            maxBound = pos;
            return;
        }
        if (pos.x < minBound.x) minBound = minBound.multiply(0, 1, 1).add(pos.x, 0, 0);
        if (pos.y < minBound.y) minBound = minBound.multiply(1, 0, 1).add(0, pos.y, 0);
        if (pos.z < minBound.z) minBound = minBound.multiply(1, 1, 0).add(0, 0, pos.z);
        if (pos.x > maxBound.x) maxBound = maxBound.multiply(0, 1, 1).add(pos.x, 0, 0);
        if (pos.y > maxBound.y) maxBound = maxBound.multiply(1, 0, 1).add(0, pos.y, 0);
        if (pos.z > maxBound.z) maxBound = maxBound.multiply(1, 1, 0).add(0, 0, pos.z);
    }

}
