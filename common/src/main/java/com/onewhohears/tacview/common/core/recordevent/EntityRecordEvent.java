package com.onewhohears.tacview.common.core.recordevent;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import com.onewhohears.tacview.client.core.ClientPlayback;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.onewhohears.tacview.common.core.KeyframeValue.UUIDV.DEFAULT_UUID_STR;

public abstract class EntityRecordEvent<E extends Entity> extends RecordEvent {
    private final UUID uuid;
    public EntityRecordEvent(@NotNull String eventId, @NotNull E entity) {
        super(eventId, UtilEntity.getLevel(entity).getGameTime());
        this.uuid = entity.getUUID();
    }
    public EntityRecordEvent(@NotNull JsonObject data) {
        super(data);
        String uuidStr = UtilParse.getStringSafe(data, "uuid", DEFAULT_UUID_STR);
        uuid = UUID.fromString(uuidStr);
    }
    @NotNull
    public JsonObject getSaveData() {
        JsonObject data = super.getSaveData();
        data.addProperty("uuid", uuid.toString());
        return data;
    }
    public void onEventPlayback(@NotNull ClientPlayback playback) {
        Entity entity = playback.getEntity(getEntityUuid());
        if (entity == null) return;
        onEventPlayback(playback, (E) entity);
    }
    protected abstract void onEventPlayback(@NotNull ClientPlayback playback, @NotNull E entity);
    @NotNull
    public UUID getEntityUuid() {
        return uuid;
    }
}
