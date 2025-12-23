package com.onewhohears.tacview.core;

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
    @NotNull public abstract T interpolate(T end, float partial);
    public void set(T value) {
        this.value = value;
    }

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
        public @NotNull Integer interpolate(Integer end, float partial) {
            return (int) ((end - get()) * partial + get());
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
        public @NotNull Long interpolate(Long end, float partial) {
            return (long) ((end - get()) * partial + get());
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
        public @NotNull Float interpolate(Float end, float partial) {
            return (end - get()) * partial + get();
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
        public @NotNull Vec3 interpolate(Vec3 end, float partial) {
            return get().lerp(end, partial);
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
        public @NotNull UUID interpolate(UUID end, float partial) {
            return get();
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
        public @NotNull String interpolate(String end, float partial) {
            return get();
        }
    }

}
