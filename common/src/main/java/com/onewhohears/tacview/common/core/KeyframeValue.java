package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onewhohears.onewholibs.util.UtilItem;
import com.onewhohears.onewholibs.util.UtilMCText;
import com.onewhohears.onewholibs.util.UtilParse;
import com.onewhohears.onewholibs.util.math.UtilAngles;
import com.onewhohears.onewholibs.util.math.UtilGeometry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.onewhohears.tacview.common.core.EntityRecorder.ID;

public abstract class KeyframeValue<T, E extends Entity> {
    public final String name;
    private final Function<E,T> entityReader;
    private final BiConsumer<E,T> entitySetter;
    protected T value = getDefault();
    public KeyframeValue(String name, Function<E,T> entityReader, BiConsumer<E,T> entitySetter) {
        this.name = name;
        this.entityReader = entityReader;
        this.entitySetter = entitySetter;
    }
    public void readFromEntity(E entity) {
        value = entityReader.apply(entity);
    }
    public void setEntity(E entity) {
        entitySetter.accept(entity, value);
    }
    public final void readFromData(JsonObject data, @Nullable T prevValue) {
        if (!data.has(name) && prevValue != null) {
            value = prevValue;
        } else {
            readFromData(data);
        }
    }
    public abstract void readFromData(JsonObject data);
    public final void writeToData(JsonObject data, @Nullable T prevValue) {
        if (prevValue != null && isEqual(prevValue)) return;
        writeToData(data);
    }
    public abstract void writeToData(JsonObject data);
    @NotNull public abstract T get();
    @NotNull public abstract T getDefault();
    public abstract void setToLerp(@NotNull T start, @NotNull T end, float partial);
    public abstract boolean isEqual(@NotNull T other);
    public void addOverlayInfo(@NotNull List<Component> overlayEntityInfo) {
        MutableComponent n = UtilMCText.literal(name+": ").setStyle(ID);
        overlayEntityInfo.add(n.append(UtilMCText.literal(getValueToString()).setStyle(EntityRecorder.VALUE)));
    }
    public String getValueToString() {
        return get()+"";
    }

    public static class BoolV<E extends Entity> extends KeyframeValue<Boolean,E> {
        public BoolV(String name, Function<E, Boolean> entityReader, BiConsumer<E, Boolean> entitySetter) {
            super(name, entityReader, entitySetter);
        }
        @Override
        public void readFromData(JsonObject data) {
            value = UtilParse.getBooleanSafe(data, name, false);
        }
        @Override
        public void writeToData(JsonObject data) {
            data.addProperty(name, value);
        }
        @Override
        public @NotNull Boolean get() {
            return value;
        }
        @Override
        public @NotNull Boolean getDefault() {
            return false;
        }
        @Override
        public void setToLerp(@NotNull Boolean start, @NotNull Boolean end, float partial) {
            value = start;
        }
        @Override
        public boolean isEqual(@NotNull Boolean other) {
            return get() == other;
        }
    }

    public static class IntV<E extends Entity> extends KeyframeValue<Integer,E> {
        public IntV(String name, Function<E, Integer> entityReader, BiConsumer<E, Integer> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull Integer getDefault() {
            return 0;
        }
        @Override
        public void setToLerp(@NotNull Integer start, @NotNull Integer end, float partial) {
            value = (int) ((end - start) * partial + start);
        }
        @Override
        public boolean isEqual(@NotNull Integer other) {
            return Objects.equals(get(), other);
        }
    }

    public static class LongV<E extends Entity> extends KeyframeValue<Long,E> {
        public LongV(String name, Function<E, Long> entityReader, BiConsumer<E, Long> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull Long getDefault() {
            return 0L;
        }
        @Override
        public void setToLerp(@NotNull Long start, @NotNull Long end, float partial) {
            value = (long) ((end - start) * partial + start);
        }
        @Override
        public boolean isEqual(@NotNull Long other) {
            return Objects.equals(get(), other);
        }
    }

    public static class FloatV<E extends Entity> extends KeyframeValue<Float,E> {
        public FloatV(String name, Function<E, Float> entityReader, BiConsumer<E, Float> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull Float getDefault() {
            return 0f;
        }
        @Override
        public void setToLerp(@NotNull Float start, @NotNull Float end, float partial) {
            value = (end - start) * partial + start;
        }
        @Override
        public boolean isEqual(@NotNull Float other) {
            return Objects.equals(get(), other);
        }
    }

    public static class AngleV<E extends Entity> extends FloatV<E> {
        public AngleV(String name, Function<E, Float> entityReader, BiConsumer<E, Float> entitySetter) {
            super(name, entityReader, entitySetter);
        }
        @Override
        public void setToLerp(@NotNull Float start, @NotNull Float end, float partial) {
            value = UtilAngles.lerpAngle(partial, start, end);
        }
    }

    public static class Vec3V<E extends Entity> extends KeyframeValue<Vec3,E> {
        public Vec3V(String name, Function<E, Vec3> entityReader, BiConsumer<E, Vec3> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull Vec3 getDefault() {
            return Vec3.ZERO;
        }
        @Override
        public void setToLerp(@NotNull Vec3 start, @NotNull Vec3 end, float partial) {
            value = start.lerp(end, partial);
        }
        @Override
        public boolean isEqual(@NotNull Vec3 other) {
            return UtilGeometry.isEqual(get(), other);
        }
        @Override
        public String getValueToString() {
            return String.format("[%.1f,%.1f,%.1f]",get().x,get().y,get().z);
        }
    }

    public static class UUIDV<E extends Entity> extends KeyframeValue<UUID,E> {
        public static final String DEFAULT_UUID_STR = "12345678-0000-0000-0000-000000000000";
        public static final UUID DEFAULT_UUID = UUID.fromString(DEFAULT_UUID_STR);
        public UUIDV(String name, Function<E, UUID> entityReader, BiConsumer<E, UUID> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull UUID getDefault() {
            return DEFAULT_UUID;
        }
        @Override
        public void setToLerp(@NotNull UUID start, @NotNull UUID end, float partial) {
            value = start;
        }
        @Override
        public boolean isEqual(@NotNull UUID other) {
            return Objects.equals(get(), other);
        }
    }

    public static class StringV<E extends Entity> extends KeyframeValue<String,E> {
        public StringV(String name, Function<E, String> entityReader, BiConsumer<E, String> entitySetter) {
            super(name, entityReader, entitySetter);
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
        public @NotNull String getDefault() {
            return "";
        }
        @Override
        public void setToLerp(@NotNull String start, @NotNull String end, float partial) {
            value = start;
        }
        @Override
        public boolean isEqual(@NotNull String other) {
            return Objects.equals(get(), other);
        }
    }

    public static class EnumV<A extends Enum<A>, E extends Entity> extends KeyframeValue<A,E> {
        private final Class<A> enumClass;
        private final A defaultValue;
        public EnumV(String name, Function<E, A> entityReader, BiConsumer<E, A> entitySetter, Class<A> enumClass) {
            super(name, entityReader, entitySetter);
            this.enumClass = enumClass;
            this.defaultValue = enumClass.getEnumConstants()[0];
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
        public @NotNull A getDefault() {
            return defaultValue;
        }
        @Override
        public void setToLerp(@NotNull A start, @NotNull A end, float partial) {
            value = start;
        }
        @Override
        public boolean isEqual(@NotNull A other) {
            return value == other;
        }
    }

    public static class ItemStackV<E extends Entity> extends KeyframeValue<ItemStack,E> {
        public ItemStackV(String name, Function<E, ItemStack> entityReader, BiConsumer<E, ItemStack> entitySetter) {
            super(name, entityReader, entitySetter);
        }
        @Override
        public void readFromData(JsonObject data) {
            JsonObject itemData = UtilParse.getJsonSafe(data, name);
            String item = UtilParse.getStringSafe(itemData, "item", "minecraft:air");
            int count = UtilParse.getIntSafe(itemData, "count", 1);
            int damage = UtilParse.getIntSafe(itemData, "damage", 0);
            String nbtStr = UtilParse.getStringSafe(itemData, "nbt", "{}");
            CompoundTag nbt = null;
            try {
                nbt = TagParser.parseTag(nbtStr);
            } catch (CommandSyntaxException e) {
                System.out.println("Could not parse nbt for item: "+nbtStr);
                e.printStackTrace();
            }
            ItemStack stack = new ItemStack(UtilItem.getItem(item, Items.AIR));
            stack.setCount(count);
            stack.setDamageValue(damage);
            if (nbt != null) stack.setTag(nbt);
            value = stack;
        }
        @Override
        public void writeToData(JsonObject data) {
            JsonObject itemData = new JsonObject();
            itemData.addProperty("item", UtilItem.getItemKeyString(value.getItem()));
            itemData.addProperty("count", value.getCount());
            itemData.addProperty("damage", value.getDamageValue());
            itemData.addProperty("nbt", value.getOrCreateTag().toString());
            data.add(name, itemData);
        }
        @Override
        public @NotNull ItemStack get() {
            return value;
        }
        @Override
        public @NotNull ItemStack getDefault() {
            return ItemStack.EMPTY;
        }
        @Override
        public void setToLerp(@NotNull ItemStack start, @NotNull ItemStack end, float partial) {
            value = start;
        }
        @Override
        public boolean isEqual(@NotNull ItemStack other) {
            return ItemStack.isSameItemSameTags(get(), other);
        }
    }

}
