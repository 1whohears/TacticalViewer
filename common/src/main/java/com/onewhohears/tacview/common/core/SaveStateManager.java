package com.onewhohears.tacview.common.core;

import com.mojang.logging.LogUtils;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.util.UtilFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
            Map<UUID, VehicleSyncData> vehicleToPlayerMap = new HashMap<>();
            ListTag playerList = nbt.getList("players", 10);
            for (int i = 0; i < playerList.size(); ++i) {
                CompoundTag playerTag = playerList.getCompound(i);
                UUID playerUUID = playerTag.getUUID("UUID");
                Entity player = level.getEntity(playerUUID);
                if (player == null) continue;
                player.load(playerTag);
                Vec3 pos = teleportToTagPos(player, playerTag, level);
                if (playerTag.contains("vehicle")) {
                    UUID vehicleUUID = playerTag.getUUID("vehicle");
                    VehicleSyncData data = new VehicleSyncData();
                    data.player = player;
                    data.playerGoalPos = pos;
                    data.playerTag = playerTag;
                    vehicleToPlayerMap.put(vehicleUUID, data);
                } else {
                    player.stopRiding();
                }
            }
            ListTag entityList = nbt.getList("entities", 10);
            for (int i = 0; i < entityList.size(); ++i) {
                CompoundTag entityTag = entityList.getCompound(i);
                loadEntityRecursive(entityTag, level, vehicleToPlayerMap);
            }
            UtilFile.writeNbtInGamePath(getSaveStateFileName(id), nbt);
        } catch (Exception e) {
            debug.accept("Failed to load Save State "+id+" because "+e.getMessage());
            e.printStackTrace();
            return false;
        }
        debug.accept("Loaded Save State "+id);
        return true;
    }

    @Nullable
    public static Entity loadEntityRecursive(CompoundTag entityTag, ServerLevel level,
                                             Map<UUID, VehicleSyncData> vehicleToPlayerMap) {
        return EntityType.loadStaticEntity(entityTag, level).map(entity -> {
            UUID oldUUID = entity.getUUID();
            UUID newUUID = oldUUID;
            Entity entityOld = level.getEntity(oldUUID);
            if (entityOld == null || entityOld.isRemoved()) {
                newUUID = UUID.randomUUID();
                entity.setUUID(newUUID);
                entityTag.putUUID("UUID", newUUID);
                level.addFreshEntity(entity);
            } else {
                entity = entityOld;
                entity.load(entityTag);
                entity.stopRiding();
            }

            if (entityTag.contains("Passengers", 9)) {
                ListTag listTag = entityTag.getList("Passengers", 10);
                for(int i = 0; i < listTag.size(); ++i) {
                    Entity entity2 = loadEntityRecursive(listTag.getCompound(i), level, vehicleToPlayerMap);
                    if (entity2 != null) {
                        entity2.startRiding(entity, true);
                    }
                }
            }

            // TODO send a packet to player client telling it to confirm that it finished teleporting
            //  and it is time to start riding. Also tell the vehicle on the client side to instantly move
            if (vehicleToPlayerMap.containsKey(oldUUID)) {
                VehicleSyncData data = vehicleToPlayerMap.get(oldUUID);
                data.vehicle = entity;
                data.vehicleGoalPos = entity.position();
                data.playerTag.putUUID("vehicle", newUUID);
                data.player.startRiding(entity);
            }

            return entity;
        }).orElse(null);
    }

    public static class VehicleSyncData {
        private VehicleSyncData() {};
        public Entity player;
        public Vec3 playerGoalPos;
        public CompoundTag playerTag;
        public Entity vehicle;
        public Vec3 vehicleGoalPos;
    }

    private static Vec3 teleportToTagPos(@NotNull Entity entity, @NotNull CompoundTag entityTag,
                                         @NotNull ServerLevel level) {
        ListTag pos = entityTag.getList("Pos", 6);
        ListTag rot = entityTag.getList("Rotation", 5);
        Vec3 p = new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
        entity.teleportTo(level, p.x, p.y, p.z, new HashSet<>(),
                rot.getFloat(0), rot.getFloat(1));
        return p;
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
