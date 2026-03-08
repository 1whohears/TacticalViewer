package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilParse;
import com.onewhohears.tacview.client.core.ClientPlayback;
import org.jetbrains.annotations.NotNull;

public abstract class RecordEvent {
    private final String eventId;
    private final long time;
    public RecordEvent(@NotNull String eventId, long gameTIme) {
        this.eventId = eventId;
        this.time = gameTIme;
    }
    public RecordEvent(@NotNull JsonObject data) {
        eventId = UtilParse.getStringSafe(data, "eventId", "");
        time = data.has("time") ? data.get("time").getAsLong() : 0;
        readSaveData(data);
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
    protected abstract void readSaveData(@NotNull JsonObject data);
    protected abstract void onEventPlayback(@NotNull ClientPlayback playback);
    @NotNull
    public String getId() {
        return eventId;
    }
    public long getTime() {
        return time;
    }
}
