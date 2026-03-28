package com.onewhohears.tacview.common.core;

import com.mojang.logging.LogUtils;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class SaveStateManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static String SAVE_STATE_PATH = "tac_view/save_states/";

    private final Set<UUID> killDuplicates = new HashSet<>();

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
        try {
            Map<UUID, Entity> vehicleToPlayerMap = new HashMap<>();
            ListTag playerList = nbt.getList("players", 10);
            for (int i = 0; i < playerList.size(); ++i) {
                CompoundTag playerTag = playerList.getCompound(i);
                UUID playerUUID = playerTag.getUUID("UUID");
                Entity player = level.getEntity(playerUUID);
                if (player == null) continue;
                player.load(playerTag);
                teleportToTagPos(player, playerTag, level);
                if (playerTag.contains("vehicle")) {
                    UUID vehicleUUID = playerTag.getUUID("vehicle");
                    vehicleToPlayerMap.put(vehicleUUID, player);
                }
            }
            ListTag entityList = nbt.getList("entities", 10);
            for (int i = 0; i < entityList.size(); ++i) {
                CompoundTag entityTag = entityList.getCompound(i);
                EntityType.loadEntityRecursive(entityTag, level, entityNew -> {
                    UUID newUUID = entityNew.getUUID();
                    Entity entityOld = level.getEntity(newUUID);
                    if (entityOld == null) {
                        level.addFreshEntity(entityNew);
                        killDuplicates.add(newUUID);
                        // TODO delete entities that have the old uuid
                    } else {
                        entityOld.load(entityTag);
                        entityNew = entityOld;
                    }
                    teleportToTagPos(entityNew, entityTag, level);
                    if (vehicleToPlayerMap.containsKey(newUUID)) {
                        Entity player = vehicleToPlayerMap.get(newUUID);
                        player.startRiding(entityNew);
                    }
                    return entityNew;
                });
            }
        } catch (Exception e) {
            debug.accept("Failed to load Save State "+id+" because "+e.getMessage());
            e.printStackTrace();
            return false;
        }
        debug.accept("Loaded Save State "+id);
        return true;
    }

    private static void teleportToTagPos(@NotNull Entity entity, @NotNull CompoundTag entityTag,
                                         @NotNull ServerLevel level) {
        ListTag pos = entityTag.getList("Pos", 6);
        ListTag rot = entityTag.getList("Rotation", 5);
        entity.teleportTo(level, pos.getDouble(0), pos.getDouble(1), pos.getDouble(2),
                new HashSet<>(), rot.getFloat(0), rot.getFloat(1));
    }

    public boolean createSaveState(@NotNull String id, @NotNull ServerLevel level,
                                   @NotNull Collection<? extends Entity> entities,
                                   @NotNull Consumer<String> debug) {
        if (UtilFile.doesFileExistGamePath(getSaveStateFileName(id))) {
            debug.accept("Could not create a new save state because the id "+id+" already exists!");
            return false;
        }
        CompoundTag nbt = serializeEntities(entities);
        nbt.putString("dimension", level.dimension().location().toString());
        UtilFile.writeNbtInGamePath(getSaveStateFileName(id), nbt);
        debug.accept("Saved new save state with id "+id);
        return true;
    }

    private static @NotNull CompoundTag serializeEntities(@NotNull Collection<? extends Entity> entities) {
        CompoundTag nbt = new CompoundTag();
        ListTag entityList = new ListTag();
        ListTag playerList = new ListTag();
        Set<Integer> savedEntities = new HashSet<>();
        for (Entity entity : entities) {
            if (UtilEntity.isPlayer(entity)) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putString("id", "minecraft:player");
                entity.saveWithoutId(playerTag);
                playerList.add(playerTag);
                Entity vehicle = entity.getVehicle();
                if (vehicle == null) continue;
                playerTag.putUUID("vehicle", vehicle.getUUID());
            }
            CompoundTag entityTag = new CompoundTag();
            Entity root = entity.getRootVehicle();
            if (!savedEntities.contains(root.getId()) && root.save(entityTag)) {
                entityList.add(entityTag);
                savedEntities.add(root.getId());
            }
        }
        nbt.put("entities", entityList);
        nbt.put("players", playerList);
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
