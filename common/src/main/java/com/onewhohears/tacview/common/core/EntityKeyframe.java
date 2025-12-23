package com.onewhohears.tacview.common.core;

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
 * All the saved data at a tick that will be replayed later. These are values that could change over time.
 */
public class EntityKeyframe<E extends Entity> {

    public final Map<String, KeyframeValue<Object,E>> values = new HashMap<>();

    public final KeyframeValue.Vec3V<E> pos = registerVec3Value("pos", Entity::position);
    public final KeyframeValue.Vec3V<E> vel = registerVec3Value("vel", Entity::getDeltaMovement);
    public final KeyframeValue.FloatV<E> xRot = registerFloatValue("xRot", Entity::getXRot);
    public final KeyframeValue.FloatV<E> yRot = registerFloatValue("yRot", Entity::getYRot);
    public final KeyframeValue.StringV<E> vehicleUUID = registerStringValue("vehicleUUID",
            entity -> entity.isPassenger() ? entity.getRootVehicle().getStringUUID() : "");

    protected long tick;

    public EntityKeyframe(@NotNull E entity) {
        tick = getGameTime(entity);
        values.forEach((name, value) -> value.readFromEntity(entity));
    }

    public EntityKeyframe(@NotNull JsonObject data) {
        tick = !data.has("tick") ? 0 : data.get("tick").getAsLong();
        values.forEach((name, value) -> value.readFromData(data));
    }

    protected EntityKeyframe() {
    }

    public static <K extends EntityKeyframe<E>, E extends Entity> void setToLerp(
            K lerpKeyframe, K start, K end, long gameTime, float partialTick) {
        float partial = calcPartial(start.tick, end.tick, gameTime, partialTick);
        lerpKeyframe.tick = gameTime;
        lerpKeyframe.values.forEach((name, value) ->
                value.setToLerp(start.values.get(name).get(), end.values.get(name).get(), partial)
        );
    }

    public final @NotNull JsonObject getSaveData() {
        JsonObject data = new JsonObject();
        addSaveData(data);
        return data;
    }

    protected void addSaveData(@NotNull JsonObject data) {
        data.addProperty("tick", tick);
        values.forEach((name, value) -> value.writeToData(data));
    }

    protected <K extends KeyframeValue<?,E>> K registerValue(K value) {
        values.put(value.name, (KeyframeValue<Object, E>) value);
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

    protected <A extends Enum<A>> KeyframeValue.EnumV<A,E> registerEnumValue(String name, Function<E, A> entityReader, Class<A> enumClass) {
        return registerValue(new KeyframeValue.EnumV<>(name, entityReader, enumClass));
    }

    public long getTick() {
        return tick;
    }

    public static long getGameTime(@NotNull Entity entity) {
        return UtilEntity.getLevel(entity).getGameTime();
    }

    public static float calcPartial(long startTick, long endTick, long gameTime, float partialTick) {
        if (gameTime < startTick) return 0;
        else if (gameTime >= endTick) return 1;
        long length = endTick - startTick;
        return (gameTime - startTick + partialTick) / length;
    }

}
