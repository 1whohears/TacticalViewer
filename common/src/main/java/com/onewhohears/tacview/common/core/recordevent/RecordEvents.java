package com.onewhohears.tacview.common.core.recordevent;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilParse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RecordEvents {

    private static final Map<String,RecordEventReader> READERS = new HashMap<>();

    public static void registerRecordEventReader(@NotNull String eventId, @NotNull RecordEventReader reader) {
        READERS.put(eventId, reader);
    }

    @Nullable
    public static RecordEvent readEvent(@NotNull JsonObject data) {
        String eventId = UtilParse.getStringSafe(data, "eventId", "");
        RecordEventReader reader = READERS.get(eventId);
        if (reader == null) return null;
        return reader.read(data);
    }

    public static void registerDefaultEventReaders() {
        registerRecordEventReader("player_attack", MoreRecordEvents.PlayerAttack::new);
        registerRecordEventReader("living_death", MoreRecordEvents.LivingDeath::new);
    }

    public interface RecordEventReader {
        @NotNull RecordEvent read(@NotNull JsonObject data);
    }
}
