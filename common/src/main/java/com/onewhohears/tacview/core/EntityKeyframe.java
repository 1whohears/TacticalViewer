package com.onewhohears.tacview.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * All of the saved data at a tick that will be replayed later. These are values that could change over time.
 */
public class EntityKeyframe<E extends Entity> {

    private final Map<String, KeyframeValue<?,E>> values = new HashMap<>();

    public final KeyframeValue.LongV<E> tick = registerLongValue("tick", EntityKeyframe::getGameTime);
    public final KeyframeValue.Vec3V<E> pos = registerVec3Value("pos", Entity::position);
    public final KeyframeValue.Vec3V<E> vel = registerVec3Value("vel", Entity::getDeltaMovement);
    public final KeyframeValue.FloatV<E> xRot = registerFloatValue("xRot", Entity::getXRot);
    public final KeyframeValue.FloatV<E> yRot = registerFloatValue("yRot", Entity::getYRot);

    public EntityKeyframe(@NotNull E entity) {
        values.forEach((name, value) -> value.readFromEntity(entity));
    }

    protected <K extends KeyframeValue<?,E>> K registerValue(K value) {
        values.put(value.name, value);
        return value;
    }

    protected KeyframeValue.IntV<E> registerIntValue(String name, Function<E, Integer> entityReader) {
        return registerValue(new KeyframeValue.IntV<>(name, entityReader));
    }

    protected KeyframeValue.LongV<E> registerLongValue(String name, Function<E, Long> entityReader) {
        return registerValue(new KeyframeValue.LongV<>(name, entityReader));
    }

    protected KeyframeValue.FloatV<E> registerFloatValue(String name, Function<E, Float> entityReader) {
        return registerValue(new KeyframeValue.FloatV<>(name, entityReader));
    }

    protected KeyframeValue.Vec3V<E> registerVec3Value(String name, Function<E, Vec3> entityReader) {
        return registerValue(new KeyframeValue.Vec3V<>(name, entityReader));
    }

    protected KeyframeValue.UUIDV<E> registerUUIDValue(String name, Function<E, UUID> entityReader) {
        return registerValue(new KeyframeValue.UUIDV<>(name, entityReader));
    }

    protected KeyframeValue.StringV<E> registerStringValue(String name, Function<E, String> entityReader) {
        return registerValue(new KeyframeValue.StringV<>(name, entityReader));
    }

    public EntityKeyframe(@NotNull JsonObject data) {
        values.forEach((name, value) -> value.readFromData(data));
    }

    public final @NotNull JsonObject getSaveData() {
        JsonObject data = new JsonObject();
        addSaveData(data);
        return data;
    }

    protected void addSaveData(@NotNull JsonObject data) {
        values.forEach((name, value) -> value.writeToData(data));
    }

    public static long getGameTime(@NotNull Entity entity) {
        return UtilEntity.getLevel(entity).getGameTime();
    }

}
