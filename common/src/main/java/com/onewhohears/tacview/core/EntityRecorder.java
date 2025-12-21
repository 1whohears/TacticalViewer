package com.onewhohears.tacview.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves all the keyframes during the recording period and also saves static information about the entity.
 */
public abstract class EntityRecorder<K extends EntityKeyframe<E>, E extends Entity> extends EntityKeyframe<E> {

    public final KeyframeValue.UUIDV<E> uuid = registerUUIDValue("uuid", Entity::getUUID);
    public final KeyframeValue.StringV<E> entityType = registerStringValue("entityType", UtilEntity::getEntityTypeId);

    public final int recordRate;

    @NotNull private final List<K> keyframes = new ArrayList<>();

    @Nullable private E entity;
    private long prevRecordTime = 0;

    public EntityRecorder(@NotNull E entity, int recordRate) {
        super(entity);
        this.entity = entity;
        if (recordRate < 1) recordRate = 1;
        this.recordRate = recordRate;
    }

    public void tick() {
        if (entity == null) return;
        long time = getGameTime(entity);
        if (time - prevRecordTime < recordRate) return;
        keyframes.add(newKeyFrame(entity));
        prevRecordTime = time;
    }

    public EntityRecorder(@NotNull JsonObject data) {
        super(data);
        recordRate = UtilParse.getIntSafe(data, "recordRate", 10);
        prevRecordTime = UtilParse.getIntSafe(data, "prevRecordTime", 0);
        JsonArray kfArray = !data.has("keyframes") ? new JsonArray() : data.get("keyframes").getAsJsonArray();
        for (int i = 0; i < kfArray.size(); ++i) {
            JsonObject kfObject = kfArray.get(i).getAsJsonObject();
            K keyframe = readKeyframe(kfObject);
            if (keyframe == null) continue;
            keyframes.add(keyframe);
        }
    }

    protected void addSaveData(@NotNull JsonObject data) {
        super.addSaveData(data);
        data.addProperty("recordRate", recordRate);
        data.addProperty("prevRecordTime", prevRecordTime);
        JsonArray kfArray = new JsonArray();
        for (K keyframe : keyframes) kfArray.add(keyframe.getSaveData());
        data.add("keyframes", kfArray);
    }

    @Nullable
    public E getEntity() {
        return entity;
    }

    @Nullable
    protected abstract K readKeyframe(@NotNull JsonObject keyframe);
    protected abstract K newKeyFrame(@NotNull E entity);

}
