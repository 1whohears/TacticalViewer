package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EntityRecorders {

    private static final Map<String,Pair<EntityRecorderFactory,EntityRecorderReader>> RECORDER_FACTORIES = new HashMap<>();
    private static final EntityRecorderFactory DEFAULT_FACTORY = MoreRecorders.DefaultEntityRecorder::new;
    private static final EntityRecorderReader DEFAULT_RECORDER = MoreRecorders.DefaultEntityRecorder::new;

    public static void registerDefaultRecorders() {
        registerEntityRecorder(EntityType.HORSE,
                (entity, recordRate) -> new MoreRecorders.LivingRec((LivingEntity) entity, recordRate),
                MoreRecorders.LivingRec::new);
        registerEntityRecorder(EntityType.PLAYER,
                (entity, recordRate) -> new PlayerRecorder((Player) entity, recordRate),
                PlayerRecorder::new);
    }

    public static void registerEntityRecorder(EntityType<?> type, EntityRecorderFactory factory, EntityRecorderReader reader) {
        RECORDER_FACTORIES.put(EntityType.getKey(type).toString(), Pair.of(factory, reader));
    }

    @NotNull
    public static EntityRecorder createEntityRecorder(@NotNull Entity entity, int recordRate) {
        String id = UtilEntity.getEntityTypeId(entity);
        EntityRecorder recorder;
        if (!RECORDER_FACTORIES.containsKey(id)) recorder = DEFAULT_FACTORY.create(entity, recordRate);
        else recorder = RECORDER_FACTORIES.get(id).getLeft().create(entity, recordRate);
        recorder.readValuesFromEntityCast(entity);
        return recorder;
    }

    @Nullable
    public static EntityRecorder readEntityRecorder(@NotNull JsonObject data) {
        String id = UtilParse.getStringSafe(data, "entityType", "");
        if (id.isEmpty()) return null;
        EntityRecorder recorder;
        if (!RECORDER_FACTORIES.containsKey(id)) recorder = DEFAULT_RECORDER.read(data);
        else recorder = RECORDER_FACTORIES.get(id).getRight().read(data);
        recorder.readValuesFromData(data);
        return recorder;
    }

    public interface EntityRecorderFactory {
        @NotNull EntityRecorder create(@NotNull Entity entity, int recordRate);
    }

    public interface EntityRecorderReader {
        @NotNull EntityRecorder read(@NotNull JsonObject data);
    }

}
