package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Function;

public abstract class KeyframeValue<T, E extends Entity> {
    public final String name;
    private final Function<E,T> entityReader;
    protected T value;
    public KeyframeValue(String name, Function<E, T> entityReader) {
        this.name = name;
        this.entityReader = entityReader;
    }
    public void readFromEntity(E entity) {
        value = entityReader.apply(entity);
    }
    public abstract void readFromData(JsonObject data);
    public abstract void writeToData(JsonObject data);
    @NotNull public abstract T get();
    public abstract void setToLerp(T start, T end, float partial);

    public static class IntV<E extends Entity> extends KeyframeValue<Integer,E> {
        public IntV(String name, Function<E, Integer> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.getIntSafe(data, name, 0);
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value);
        }
        @Override
        public @NotNull Integer get() {
            return value;
        }
        @Override
        public void setToLerp(Integer start, Integer end, float partial) {
            value = (int) ((end - start) * partial + start);
        }
    }

    public static class LongV<E extends Entity> extends KeyframeValue<Long,E> {
        public LongV(String name, Function<E, Long> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = !data.has(name) ? 0 : data.get(name).getAsLong();
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value);
        }
        @Override
        public @NotNull Long get() {
            return value;
        }
        @Override
        public void setToLerp(Long start, Long end, float partial) {
            value = (long) ((end - get()) * partial + get());
        }
    }

    public static class FloatV<E extends Entity> extends KeyframeValue<Float,E> {
        public FloatV(String name, Function<E, Float> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.getFloatSafe(data, name, 0);
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value);
        }
        @Override
        public @NotNull Float get() {
            return value;
        }
        @Override
        public void setToLerp(Float start, Float end, float partial) {
            value = (end - get()) * partial + get();
        }
    }

    public static class Vec3V<E extends Entity> extends KeyframeValue<Vec3,E> {
        public Vec3V(String name, Function<E, Vec3> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.readVec3(data, name);
        }
        @Override
        public void writeToData(JsonObject data) {
            UtilParse.writeVec3(data, name, value);
        }
        @Override
        public @NotNull Vec3 get() {
            return value;
        }
        @Override
        public void setToLerp(Vec3 start, Vec3 end, float partial) {
            value = get().lerp(end, partial);
        }
    }

    public static class UUIDV<E extends Entity> extends KeyframeValue<UUID,E> {
        public UUIDV(String name, Function<E, UUID> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UUID.fromString(UtilParse.getStringSafe(data, name, ""));
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value.toString());
        }
        @Override
        public @NotNull UUID get() {
            return value;
        }
        @Override
        public void setToLerp(UUID start, UUID end, float partial) {
            value = start;
        }
    }

    public static class StringV<E extends Entity> extends KeyframeValue<String,E> {
        public StringV(String name, Function<E, String> entityReader) {
            super(name, entityReader);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.getStringSafe(data, name, "");
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value);
        }
        @Override
        public @NotNull String get() {
            return value;
        }
        @Override
        public void setToLerp(String start, String end, float partial) {
            value = start;
        }
    }

    public static class EnumV<A extends Enum<A>, E extends Entity> extends KeyframeValue<A,E> {
        private final Class<A> enumClass;
        public EnumV(String name, Function<E, A> entityReader, Class<A> enumClass) {
            super(name, entityReader);
            this.enumClass = enumClass;
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.getEnumSafe(data, name, enumClass);
        }
        @Override
        public void writeToData(JsonObject data) {
            UtilParse.writeEnum(data, name, value);
        }
        @Override
        public @NotNull A get() {
            return value;
        }
        @Override
        public void setToLerp(A start, A end, float partial) {
            value = start;
        }
    }

}
