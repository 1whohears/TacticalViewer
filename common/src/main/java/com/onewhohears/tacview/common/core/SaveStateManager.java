package com.onewhohears.tacview.common.core;

import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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
        if (!nbt.contains("entities")) {
            debug.accept("Could not load save state because doesn't have the right data.");
            return false;
        }
        ListTag entityList = nbt.getList("entities", 10);
        for (int i = 0; i < entityList.size(); ++i) {
            CompoundTag entityTag = entityList.getCompound(i);
            UUID uuid = entityTag.getUUID("UUID");
            Entity entity = level.getEntity(uuid);
            if (entity == null) {
                entity = EntityType.loadEntityRecursive(entityTag, level, entityX -> entityX);
                // TODO delete entities that have the old uuid
            } else {
                // TODO recursively load passengers
                entity.load(entityTag);
            }
        }
        return true;
    }

    public boolean createSaveState(@NotNull String id, @NotNull List<Entity> entities,
                                   @NotNull Consumer<String> debug) {
        if (UtilFile.doesFileExistGamePath(getSaveStateFileName(id))) {
            debug.accept("Could not create a new save state because the id "+id+" already exists!");
            return false;
        }
        CompoundTag nbt = new CompoundTag();
        ListTag entityList = new ListTag();
        for (Entity entity : entities) {
            CompoundTag entityTag = new CompoundTag();
            if (entity.save(entityTag)) entityList.add(entityTag);
        }
        nbt.put("entities", entityList);
        return true;
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
