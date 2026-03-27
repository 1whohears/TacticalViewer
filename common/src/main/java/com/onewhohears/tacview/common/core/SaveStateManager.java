package com.onewhohears.tacview.common.core;

import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SaveStateManager {

    public static String SAVE_STATE_PATH = "tac_view/save_states/";

    public boolean loadSaveState(@NotNull String id, @NotNull ServerLevel level,
                                 @NotNull Consumer<String> debug) {
        CompoundTag nbt = UtilFile.readNbtInGamePath(getSaveStateFileName(id));
        if (nbt.isEmpty()) {
            debug.accept("Could not load save state because it is empty or the file doesn't exist.");
            return false;
        }
        if (!nbt.contains("dimension")) {
            debug.accept("Could not load save state because doesn't have the right data.");
            return false;
        }
        String dimension = nbt.getString("dimension");
        if (!dimension.equals(level.dimension().location().toString())) {
            debug.accept("This save state requires you to be in the dimension "+dimension);
            return false;
        }
        if (!nbt.contains("entities")) {
            debug.accept("Could not load save state because doesn't have the right data.");
            return false;
        }
        ListTag entityList = nbt.getList("entities", 10);
        for (int i = 0; i < entityList.size(); ++i) {
            CompoundTag entityTag = entityList.getCompound(i);
            EntityType.loadEntityRecursive(entityTag, level, entityNew -> {
                Entity entityOld = level.getEntity(entityNew.getUUID());
                if (entityOld == null) {
                    if (!UtilEntity.isPlayer(entityNew)) {
                        level.addFreshEntity(entityNew);
                        // TODO delete entities that have the old uuid
                    }
                } else {
                    entityOld.load(entityTag);
                    entityOld.moveTo(entityNew.getX(), entityNew.getY(), entityNew.getZ(),
                            entityNew.getXRot(), entityNew.getYRot());
                    entityNew = entityOld;
                }
                return entityNew;
            });
        }
        return true;
    }

    public boolean createSaveState(@NotNull String id, @NotNull ServerLevel level,
                                   @NotNull List<Entity> entities,
                                   @NotNull Consumer<String> debug) {
        if (UtilFile.doesFileExistGamePath(getSaveStateFileName(id))) {
            debug.accept("Could not create a new save state because the id "+id+" already exists!");
            return false;
        }
        CompoundTag nbt = serializeEntities(entities);
        nbt.putString("dimension", level.dimension().location().toString());
        UtilFile.writeNbtInGamePath(getSaveStateFileName(id), nbt);
        return true;
    }

    private static @NotNull CompoundTag serializeEntities(@NotNull List<Entity> entities) {
        CompoundTag nbt = new CompoundTag();
        ListTag entityList = new ListTag();
        Set<Integer> savedEntities = new HashSet<>();
        for (Entity entity : entities) {
            Entity root = entity.getRootVehicle();
            CompoundTag entityTag = new CompoundTag();
            if (!savedEntities.contains(root.getId()) && root.save(entityTag)) {
                entityList.add(entityTag);
                savedEntities.add(root.getId());
            }
        }
        nbt.put("entities", entityList);
        return nbt;
    }

    public Set<String> getSaveStateIds() {
        return UtilFile.getFileNamesEndingWithInGamePath(SAVE_STATE_PATH, ".nbt");
    }

    public static String getSaveStateFileName(String id) {
        return SAVE_STATE_PATH + id + ".nbt";
    }

    private static SaveStateManager INSTANCE = null;

    public static SaveStateManager get() {
        if (INSTANCE == null) INSTANCE = new SaveStateManager();
        return INSTANCE;
    }

    private SaveStateManager() {}
}
