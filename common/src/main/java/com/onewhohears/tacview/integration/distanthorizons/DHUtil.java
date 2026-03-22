package com.onewhohears.tacview.integration.distanthorizons;

import com.mojang.logging.LogUtils;
import com.onewhohears.tacview.client.core.HeightMapData;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DHUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static HeightMapData getHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod) {
        Iterable<IDhApiLevelWrapper> levelWrappers = DhApi.Delayed.worldProxy.getAllLoadedLevelsWithDimensionNameLike(
                level.dimension().location().getPath());

        Vec3 size = maxBound.subtract(minBound);
        int xLength = (int)Math.ceil(size.x / lod);
        int zLength = (int)Math.ceil(size.z / lod);
        int[][] heightMap = new int[xLength][zLength];
        int[][] colorMap = new int[xLength][zLength];

        if (!levelWrappers.iterator().hasNext()) {
            LOGGER.warn("DHUtil returned an empty height map because no level wrapper was found!");
            return new HeightMapData(xLength, zLength);
        }
        IDhApiLevelWrapper levelWrapper = levelWrappers.iterator().next();

        boolean oneGoodPayload = false;
        int yPos = (int) maxBound.y;
        for (int x = 0; x < heightMap.length; ++x) {
            for (int z = 0; z < heightMap[x].length; ++z) {
                int xPos = (int) (minBound.x + x * lod);
                int zPos = (int) (minBound.z + z * lod);
                DhApiResult<DhApiTerrainDataPoint> point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, yPos, zPos, getTerrainCache());
                if (!point.success || point.payload == null) continue; // FIXME why does this fail on large maps?
                oneGoodPayload = true;
                int h;
                if (point.payload.topYBlockPos > 200) {
                    h = point.payload.bottomYBlockPos;
                } else {
                    h = point.payload.topYBlockPos;
                }
                heightMap[x][z] = h;
                h -= 1;
                point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, h, zPos, getTerrainCache());
                int color = getColor(point.payload.blockStateWrapper, level, xPos, h, yPos);
                colorMap[x][z] = color;
            }
        }
        if (!oneGoodPayload) {
            LOGGER.warn("DHUtil returned an empty height map because no DHApi data point calls were successful!");
        }
        return new HeightMapData(heightMap, colorMap);
    }

    private static IDhApiTerrainDataCache TERRAIN_CACHE = null;

    private static IDhApiTerrainDataCache getTerrainCache() {
        if (TERRAIN_CACHE == null) {
            TERRAIN_CACHE = DhApi.Delayed.terrainRepo.createSoftCache();
        }
        return TERRAIN_CACHE;
    }

    public static int getColor(IDhApiBlockStateWrapper state, Level level, int x, int y, int z) {
        if (!(state.getWrappedMcObject() instanceof BlockState blockState)) return 0;
        BlockPos blockPos = new BlockPos(x, y, z);
        return blockState.getMapColor(level, blockPos).calculateRGBColor(MapColor.Brightness.NORMAL);
    }

}
