package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class RecordEvent<K extends EntityKeyframe<E>, E extends Entity> {
    private final String eventId;
    private long time;
    public RecordEvent(@NotNull String eventId, long gameTIme) {
        this.eventId = eventId;
        this.time = gameTIme;
    }
    @NotNull
    public final JsonObject getSaveData() {
        JsonObject data = new JsonObject();
        data.addProperty("eventId", getId());
        data.addProperty("time", getTime());
        addSaveData(data);
        return data;
    }
    protected abstract void addSaveData(@NotNull JsonObject data);
    public final void loadSaveData(@NotNull JsonObject data) {
        time = data.has("time") ? data.get("time").getAsLong() : 0;
        readSaveData(data);
    }
    protected abstract void readSaveData(@NotNull JsonObject data);
    protected abstract void onEventPlayback(E entity, K keyframe);
    @NotNull
    public String getId() {
        return eventId;
    }
    public long getTime() {
        return time;
    }
}
