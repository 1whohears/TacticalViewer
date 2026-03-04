package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * All the saved data at a tick that will be replayed later. These are values that could change over time.
 */
public class EntityKeyframe<E extends Entity> {

    public final Map<String, KeyframeValue<Object,E>> values = new HashMap<>();

    public final KeyframeValue.Vec3V<E> pos = registerVec3Value("pos", Entity::position, Entity::setPos);
    public final KeyframeValue.Vec3V<E> vel = registerVec3Value("vel", entity -> {
        if (entity.onGround()) return entity.getDeltaMovement().multiply(1, 0, 1);
        else return entity.getDeltaMovement();
    }, Entity::setDeltaMovement);
    public final KeyframeValue.FloatV<E> xRot = registerAngleValue("xRot", Entity::getXRot, Entity::setXRot);
    public final KeyframeValue.FloatV<E> yRot = registerAngleValue("yRot", Entity::getYRot, Entity::setYRot);
    public final KeyframeValue.StringV<E> vehicleUUID = registerStringValue("vehicleUUID",
            entity -> entity.isPassenger() ? entity.getRootVehicle().getStringUUID() : "",
            (entity, value) -> {});

    protected long tick;

    public EntityKeyframe(@NotNull E entity) {
        tick = getGameTime(entity);
    }

    public EntityKeyframe(@NotNull JsonObject data) {
        tick = !data.has("tick") ? 0 : data.get("tick").getAsLong();
    }

    protected EntityKeyframe() {
    }

    public void writeToFakeEntity(@NotNull E entity) {
        values.forEach((name, value) -> value.setEntity(entity));
    }

    public void readValuesFromEntityCast(@NotNull Entity entity) {
        E e = (E) entity;
        values.forEach((name, value) -> value.readFromEntity(e));
    }

    public void readValuesFromEntity(@NotNull E entity) {
        values.forEach((name, value) -> value.readFromEntity(entity));
    }

    public void readValuesFromData(@NotNull JsonObject data) {
        values.forEach((name, value) -> value.readFromData(data));
    }

    public static <K extends EntityKeyframe<E>, E extends Entity> void setToLerp(
            K lerpKeyframe, K start, K end, long gameTime, float partialTick) {
        float partial = calcPartial(start.tick, end.tick, gameTime, partialTick);
        lerpKeyframe.tick = Math.min(gameTime, end.tick);
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

    protected KeyframeValue.IntV<E> registerIntValue(String name, Function<E, Integer> entityReader,
                                                     BiConsumer<E, Integer> entitySetter) {
        return registerValue(new KeyframeValue.IntV<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.LongV<E> registerLongValue(String name, Function<E, Long> entityReader,
                                                       BiConsumer<E, Long> entitySetter) {
        return registerValue(new KeyframeValue.LongV<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.FloatV<E> registerFloatValue(String name, Function<E, Float> entityReader,
                                                         BiConsumer<E, Float> entitySetter) {
        return registerValue(new KeyframeValue.FloatV<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.AngleV<E> registerAngleValue(String name, Function<E, Float> entityReader,
                                                         BiConsumer<E, Float> entitySetter) {
        return registerValue(new KeyframeValue.AngleV<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.Vec3V<E> registerVec3Value(String name, Function<E, Vec3> entityReader,
                                                       BiConsumer<E, Vec3> entitySetter) {
        return registerValue(new KeyframeValue.Vec3V<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.UUIDV<E> registerUUIDValue(String name, Function<E, UUID> entityReader,
                                                       BiConsumer<E, UUID> entitySetter) {
        return registerValue(new KeyframeValue.UUIDV<>(name, entityReader, entitySetter));
    }

    protected KeyframeValue.StringV<E> registerStringValue(String name, Function<E, String> entityReader,
                                                           BiConsumer<E, String> entitySetter) {
        return registerValue(new KeyframeValue.StringV<>(name, entityReader, entitySetter));
    }

    protected <A extends Enum<A>> KeyframeValue.EnumV<A,E> registerEnumValue(String name, Function<E, A> entityReader,
                                                                             BiConsumer<E, A> entitySetter, Class<A> enumClass) {
        return registerValue(new KeyframeValue.EnumV<>(name, entityReader, entitySetter, enumClass));
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
        float length = endTick - startTick;
        return (gameTime + partialTick - startTick) / length;
    }

}
