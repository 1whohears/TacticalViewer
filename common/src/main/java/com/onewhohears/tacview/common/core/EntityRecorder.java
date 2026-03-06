package com.onewhohears.tacview.common.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import com.onewhohears.onewholibs.util.math.QuaternionF;
import com.onewhohears.onewholibs.util.math.Vec3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Saves all the keyframes during the recording period and also saves static information about the entity.
 */
public abstract class EntityRecorder<K extends EntityKeyframe<E>, E extends Entity> extends EntityKeyframe<E> {

    public final KeyframeValue.UUIDV<E> uuid = registerUUIDValue("uuid", Entity::getUUID, (entity, value) -> {});
    public final KeyframeValue.StringV<E> entityType = registerStringValue("entityType", UtilEntity::getEntityTypeId, (entity, value) -> {});
    public final KeyframeValue.StringV<E> name = registerStringValue("name", Entity::getScoreboardName, (entity, value) -> {});

    public final int recordRate;

    @NotNull private final List<K> keyframes = new ArrayList<>();
    @NotNull private final K lerpKeyframe = emptyKeyframe();
    @NotNull private final BiFunction<ServerLevel,UUID,E> entityFinder;

    @Nullable private E entity;
    private long prevRecordTime = 0;

    public K interpolate(long gameTime, float partialTick) {
        Pair<K,K> surround = findSurroundingKeyframes(gameTime);
        return interpolate(surround.first, surround.second, gameTime, partialTick);
    }

    public Pair<K,K> findSurroundingKeyframes(long gameTime) {
        if (keyframes.isEmpty()) {
            return Pair.of(lerpKeyframe, lerpKeyframe);
        }
        if (keyframes.size() == 1 || gameTime <= keyframes.get(0).tick) {
            return Pair.of(keyframes.get(0), keyframes.get(0));
        }
        int maxIndex = keyframes.size() - 1;
        if (gameTime >= keyframes.get(maxIndex).tick) {
            return Pair.of(keyframes.get(maxIndex), keyframes.get(maxIndex));
        }
        for (int i = 0; i < keyframes.size()-1; ++i) {
            if (gameTime >= keyframes.get(i).tick && gameTime < keyframes.get(i+1).tick) {
                return Pair.of(keyframes.get(i), keyframes.get(i+1));
            }
        }
        return Pair.of(lerpKeyframe, lerpKeyframe);
    }

    public K interpolate(K start, K end, long gameTime, float partialTick) {
        EntityKeyframe.setToLerp(lerpKeyframe, start, end, gameTime, partialTick);
        return lerpKeyframe;
    }

    public final void tickRecord(@NotNull RecordingSession session, @NotNull ServerLevel level) {
        long time = level.getGameTime();
        if (time - prevRecordTime < recordRate) return;
        if (entity == null) {
            entity = entityFinder.apply(level, uuid.get());
            if (entity == null) {
                prevRecordTime = time;
                return;
            }
        }
        if (!shouldRecord(entity)) {
            entity = null;
            return;
        }
        session.updatePosBounds(entity.position());
        extraRecordLogic(session, entity);
        addNewKeyframe(entity);
        prevRecordTime = time;
    }

    protected void addNewKeyframe(@NotNull E entity) {
        K keyframe = newKeyframe(entity);
        keyframe.readValuesFromEntity(entity);
        keyframes.add(keyframe);
    }

    protected void extraRecordLogic(@NotNull RecordingSession session, @NotNull E entity) {

    }

    public EntityRecorder(@NotNull E entity, int recordRate, @NotNull BiFunction<ServerLevel,UUID,E> entityFinder) {
        super(entity);
        removeParentValues();
        this.entity = entity;
        this.entityFinder = entityFinder;
        if (recordRate < 1) recordRate = 1;
        this.recordRate = recordRate;
    }

    public EntityRecorder(@NotNull JsonObject data, @NotNull BiFunction<ServerLevel,UUID,E> entityFinder) {
        super(data);
        removeParentValues();
        recordRate = UtilParse.getIntSafe(data, "recordRate", 10);
        prevRecordTime = UtilParse.getIntSafe(data, "prevRecordTime", 0);
        this.entityFinder = entityFinder;
        JsonArray kfArray = !data.has("keyframes") ? new JsonArray() : data.get("keyframes").getAsJsonArray();
        for (int i = 0; i < kfArray.size(); ++i) {
            JsonObject kfObject = kfArray.get(i).getAsJsonObject();
            K keyframe = readKeyframe(kfObject);
            if (keyframe == null) continue;
            keyframe.readValuesFromData(kfObject, null);
            keyframes.add(keyframe);
        }
    }

    private void removeParentValues() {
        values.remove("pos");
        values.remove("vel");
        values.remove("xRot");
        values.remove("yRot");
        values.remove("vehicleUUID");
    }

    protected void addSaveData(@NotNull JsonObject data) {
        super.addSaveData(data);
        data.remove("tick");
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
    protected abstract K newKeyframe(@NotNull E entity);
    protected abstract K emptyKeyframe();
    protected boolean shouldRecord(@NotNull E entity) {
        return !entity.isRemoved();
    }

    public void onPlaybackRender(@NotNull E entity, PoseStack stack, float yaw, @NotNull Vec3 renderPos,
                                 float partialTick, MultiBufferSource buffer, int packedLight) {

    }

    public void onPlaybackTick(@NotNull E entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.xOld = entity.getX();
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();
        entity.xRotO = entity.getXRot();
        entity.yRotO = entity.getYRot();
    }

    public void onPlaybackVehicleTick(@NotNull E vehicle, @NotNull Entity passenger) {
        if (!passenger.isPassenger() || !passenger.getRootVehicle().equals(vehicle)) {
            passenger.startRiding(vehicle, true);
        }
    }

    public void onPlaybackEntitySetup(@NotNull E entity) {

    }

    public void onRecordingStart(@NotNull RecordingSession session) {

    }

    public void onRecordingFinish(@NotNull RecordingSession session) {

    }

}
