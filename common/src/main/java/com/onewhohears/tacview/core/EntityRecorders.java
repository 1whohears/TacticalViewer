package com.onewhohears.tacview.core;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.UtilParse;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EntityRecorders {

    private static final Map<String,Pair<EntityRecorderFactory,EntityRecorderReader>> RECORDER_FACTORIES = new HashMap<>();

    public static void registerDefaultRecorders() {
        registerEntityRecorder(EntityType.HORSE,
                (entity, recordRate) -> new MoreRecorders.LivingRec((LivingEntity) entity, recordRate),
                MoreRecorders.LivingRec::new);
        registerEntityRecorder(EntityType.PLAYER,
                (entity, recordRate) -> new MoreRecorders.PlayerRec((Player) entity, recordRate),
                MoreRecorders.PlayerRec::new);
    }

    public static void registerEntityRecorder(EntityType<?> type, EntityRecorderFactory factory, EntityRecorderReader reader) {
        RECORDER_FACTORIES.put(EntityType.getKey(type).toString(), Pair.of(factory, reader));
    }

    @Nullable
    public static EntityRecorder createEntityRecorder(Entity entity, int recordRate) {
        String id = UtilEntity.getEntityTypeId(entity);
        if (!RECORDER_FACTORIES.containsKey(id)) return null;
        return RECORDER_FACTORIES.get(id).getLeft().create(entity, recordRate);
    }

    @Nullable
    public static EntityRecorder readEntityRecorder(JsonObject data) {
        String id = UtilParse.getStringSafe(data, "entityType", "");
        if (id.isEmpty()) return null;
        if (!RECORDER_FACTORIES.containsKey(id)) return null;
        return RECORDER_FACTORIES.get(id).getRight().read(data);
    }

    public interface EntityRecorderFactory {
        EntityRecorder create(Entity entity, int recordRate);
    }

    public interface EntityRecorderReader {
        EntityRecorder read(JsonObject data);
    }

}
