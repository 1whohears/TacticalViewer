package com.onewhohears.tacview.integration.distanthorizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public class DHUtil {

    public static int[][] getHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound) {
        Iterable<IDhApiLevelWrapper> levelWrappers = DhApi.Delayed.worldProxy.getAllLoadedLevelsWithDimensionNameLike(
                level.dimension().location().getPath());

        Vec3 size = maxBound.subtract(minBound);
        int[][] heightMap = new int[(int)Math.ceil(size.x)][(int)Math.ceil(size.z)];

        if (!levelWrappers.iterator().hasNext()) return heightMap;
        IDhApiLevelWrapper levelWrapper = levelWrappers.iterator().next();

        int yPos = (int) maxBound.y;
        for (int x = 0; x < heightMap.length; ++x) {
            for (int z = 0; z < heightMap[x].length; ++z) {
                int xPos = (int) (minBound.x + x);
                int zPos = (int) (minBound.z + z);
                DhApiResult<DhApiTerrainDataPoint> point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, yPos, zPos, getTerrainCache());
                if (!point.success) continue;
                heightMap[x][z] = point.payload.topYBlockPos;
            }
        }
        return heightMap;
    }

    private static IDhApiTerrainDataCache TERRAIN_CACHE = null;

    private static IDhApiTerrainDataCache getTerrainCache() {
        if (TERRAIN_CACHE == null) {
            TERRAIN_CACHE = DhApi.Delayed.terrainRepo.createSoftCache();
        }
        return TERRAIN_CACHE;
    }

}
