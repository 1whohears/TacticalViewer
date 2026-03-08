package com.onewhohears.tacview.common.core;

import com.onewhohears.tacview.client.core.ClientPlayback;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class EntityRecordEvent<E extends Entity> extends RecordEvent {
    @NotNull private final UUID uuid;
    public EntityRecordEvent(@NotNull String eventId, long gameTIme, @NotNull E entity, @NotNull UUID uuid) {
        super(eventId, gameTIme);
        this.uuid = uuid;
    }
    protected void onEventPlayback(@NotNull ClientPlayback playback) {
        Entity entity = playback.getEntity(getEntityUuid());
        if (entity == null) return;
        onEventPlayback(playback, (E) entity);
    }
    protected abstract void onEventPlayback(@NotNull ClientPlayback playback, @NotNull E entity);
    public UUID getEntityUuid() {
        return uuid;
    }
}
